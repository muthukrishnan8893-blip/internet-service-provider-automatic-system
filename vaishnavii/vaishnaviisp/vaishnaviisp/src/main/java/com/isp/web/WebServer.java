package com.isp.web;

import com.isp.model.Customer;
import com.isp.model.Ticket;
import com.isp.model.DeviceConnection;
import com.isp.repo.CustomerRepository;
import com.isp.repo.TicketRepository;
import com.isp.repo.UsageRepository;
import com.isp.repo.DeviceConnectionRepository;
import com.isp.service.*;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// PDFBox (for invoice generation)
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

/**
 * Minimal HTTP server (no external deps) exposing REST-like endpoints for the ISP system.
 * This is a lightweight alternative to full Spring Boot, suitable for demo purposes.
 */
public class WebServer {
    private final CustomerService customerService;
    private final UsageService usageService;
    private final TicketService ticketService;
    private final HotspotService hotspotService;
    private final BillingService billingService;

    public WebServer() {
        // Wire repositories and services
        CustomerRepository customerRepo = new CustomerRepository();
        UsageRepository usageRepo = new UsageRepository();
        TicketRepository ticketRepo = new TicketRepository();
        DeviceConnectionRepository deviceRepo = new DeviceConnectionRepository();
        this.customerService = new CustomerService(customerRepo);
        this.usageService = new UsageService(usageRepo);
        this.ticketService = new TicketService(ticketRepo);
        this.hotspotService = new HotspotService(deviceRepo);
        this.billingService = new BillingService(customerRepo, usageRepo);
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        WebServer server = new WebServer();
        server.start(port);
        System.out.println("Web server started on http://localhost:" + port);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

    // Static UI at root: serve files from src/main/resources/public
    server.createContext("/", new StaticHandler("/public", "index.html"));

        server.createContext("/api/health", exchange -> ok(exchange, json("status", "ok")));

        // Customers
        server.createContext("/api/customers", new CustomersHandler());
        // Usage (record & query)
        server.createContext("/api/usage", new UsageHandler());
        // Tickets
        server.createContext("/api/tickets", new TicketsHandler());
        // Hotspot
        server.createContext("/api/hotspot", new HotspotHandler());
        // Billing
        server.createContext("/api/billing", new BillingHandler());

        server.setExecutor(null);
        server.start();
    }

    // Handlers
    private static class StaticHandler implements HttpHandler {
        private final String resourceBase;
        private final String indexFile;

        StaticHandler(String resourceBase, String indexFile) {
            this.resourceBase = resourceBase;
            this.indexFile = indexFile;
        }

        @Override public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();
            if (path.startsWith("/api/")) { // safety: do not serve API through static handler
                notFound(ex, msg("Unknown path"));
                return;
            }
            String resPath = resourceBase + ("/".equals(path) ? "/" + indexFile : path);
            if (resPath.endsWith("/")) {
                resPath += indexFile;
            }
            try (InputStream is = WebServer.class.getResourceAsStream(resPath)) {
                if (is == null) {
                    notFound(ex, msg("Not found"));
                    return;
                }
                byte[] bytes = is.readAllBytes();
                Headers h = ex.getResponseHeaders();
                h.set("Content-Type", contentTypeFromPath(resPath));
                ex.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
            }
        }
    }

    private class CustomersHandler implements HttpHandler {
        private final Pattern byId = Pattern.compile("^/api/customers/([^/]+)$");

        @Override public void handle(HttpExchange ex) throws IOException {
            setJson(ex);
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if ("GET".equalsIgnoreCase(method)) {
                Matcher m = byId.matcher(path);
                if (m.matches()) {
                    String id = m.group(1);
                    var c = customerService.findById(id);
                    if (c.isPresent()) ok(ex, customerToJson(c.get())); else notFound(ex, msg("Customer not found"));
                } else if ("/api/customers".equals(path)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("[");
                    boolean first = true;
                    for (Customer c : customerService.listAll()) {
                        if (!first) sb.append(',');
                        sb.append(customerToJson(c));
                        first = false;
                    }
                    sb.append("]");
                    ok(ex, sb.toString());
                } else {
                    notFound(ex, msg("Unknown path"));
                }
            } else if ("POST".equalsIgnoreCase(method) && "/api/customers".equals(path)) {
                Map<String, String> form = readForm(ex);
                String name = form.get("name");
                String email = form.get("email");
                if (isBlank(name) || isBlank(email)) { badRequest(ex, msg("name and email required")); return; }
                Customer c = customerService.createCustomer(name, email);
                created(ex, customerToJson(c));
            } else if ("DELETE".equalsIgnoreCase(method)) {
                Matcher m = byId.matcher(path);
                if (m.matches()) {
                    customerService.deleteCustomer(m.group(1));
                    ok(ex, msg("deleted"));
                } else badRequest(ex, msg("invalid customer id"));
            } else {
                methodNotAllowed(ex);
            }
        }
    }

    private class UsageHandler implements HttpHandler {
        private final Pattern byCustomer = Pattern.compile("^/api/usage/([^/]+)$");
        @Override public void handle(HttpExchange ex) throws IOException {
            setJson(ex);
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if ("POST".equalsIgnoreCase(method) && "/api/usage".equals(path)) {
                Map<String, String> form = readForm(ex);
                String customerId = form.get("customerId");
                String gbStr = form.get("gigabytes");
                if (isBlank(customerId) || isBlank(gbStr)) { badRequest(ex, msg("customerId and gigabytes required")); return; }
                try {
                    double gb = Double.parseDouble(gbStr);
                    var u = usageService.recordUsage(customerId, gb);
                    ok(ex, "{\"id\":\""+u.getId()+"\",\"customerId\":\""+u.getCustomerId()+"\",\"gigabytes\":"+u.getGigabytes()+"}");
                } catch (NumberFormatException nfe) {
                    badRequest(ex, msg("gigabytes must be a number"));
                }
            } else if ("GET".equalsIgnoreCase(method)) {
                Matcher m = byCustomer.matcher(path);
                if (m.matches()) {
                    String customerId = m.group(1);
                    var list = usageService.getUsageForCustomer(customerId);
                    StringBuilder sb = new StringBuilder("[");
                    for (int i=0;i<list.size();i++) {
                        var u = list.get(i);
                        if (i>0) sb.append(',');
                        sb.append("{\"id\":\"").append(u.getId()).append("\",\"customerId\":\"").append(u.getCustomerId()).append("\",\"gigabytes\":").append(u.getGigabytes()).append("}");
                    }
                    sb.append("]");
                    ok(ex, sb.toString());
                } else badRequest(ex, msg("invalid path"));
            } else methodNotAllowed(ex);
        }
    }

    private class TicketsHandler implements HttpHandler {
        private final Pattern byCustomer = Pattern.compile("^/api/tickets/customer/([^/]+)$");
        private final Pattern byIdStatus = Pattern.compile("^/api/tickets/([^/]+)/status$");
        @Override public void handle(HttpExchange ex) throws IOException {
            setJson(ex);
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if ("POST".equalsIgnoreCase(method) && "/api/tickets".equals(path)) {
                Map<String,String> form = readForm(ex);
                String customerId = form.get("customerId");
                String description = form.get("description");
                if (isBlank(customerId) || isBlank(description)) { badRequest(ex, msg("customerId and description required")); return; }
                var t = ticketService.createTicket(customerId, description);
                ok(ex, "{\"id\":\""+t.getId()+"\",\"status\":\""+t.getStatus()+"\"}");
            } else if ("GET".equalsIgnoreCase(method)) {
                Matcher m = byCustomer.matcher(path);
                if (m.matches()) {
                    String customerId = m.group(1);
                    var list = ticketService.findByCustomer(customerId);
                    StringBuilder sb = new StringBuilder("[");
                    for (int i=0;i<list.size();i++) {
                        var t = list.get(i);
                        if (i>0) sb.append(',');
                        sb.append("{\"id\":\"").append(t.getId()).append("\",\"status\":\"").append(t.getStatus()).append("\",\"description\":\"").append(escape(t.getDescription())).append("\"}");
                    }
                    sb.append("]");
                    ok(ex, sb.toString());
                } else badRequest(ex, msg("invalid path"));
            } else if (("POST".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method))) {
                Matcher m = byIdStatus.matcher(path);
                if (m.matches()) {
                    Map<String,String> form = readForm(ex);
                    String st = form.get("status");
                    try {
                        Ticket.Status status = Ticket.Status.valueOf(st);
                        ticketService.updateTicketStatus(m.group(1), status);
                        ok(ex, msg("updated"));
                    } catch (Exception e) {
                        badRequest(ex, msg("invalid status"));
                    }
                } else methodNotAllowed(ex);
            } else methodNotAllowed(ex);
        }
    }

    private class HotspotHandler implements HttpHandler {
        private final Pattern customerHistory = Pattern.compile("^/api/hotspot/customer/([^/]+)/history$");
        private final Pattern customerActive = Pattern.compile("^/api/hotspot/customer/([^/]+)/active$");
        private final Pattern allActive = Pattern.compile("^/api/hotspot/active$");
        private final Pattern usageUpdate = Pattern.compile("^/api/hotspot/([^/]+)/usage$");
        private final Pattern disconnect = Pattern.compile("^/api/hotspot/([^/]+)/disconnect$");
        @Override public void handle(HttpExchange ex) throws IOException {
            setJson(ex);
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if ("POST".equalsIgnoreCase(method) && "/api/hotspot/connect".equals(path)) {
                Map<String,String> form = readForm(ex);
                String customerId = form.get("customerId");
                String deviceName = form.get("deviceName");
                String mac = form.get("macAddress");
                if (isBlank(customerId) || isBlank(deviceName) || isBlank(mac)) { badRequest(ex, msg("customerId, deviceName, macAddress required")); return; }
                var dc = hotspotService.connectDevice(customerId, deviceName, mac);
                ok(ex, "{\"id\":\""+dc.getId()+"\",\"deviceName\":\""+escape(dc.getDeviceName())+"\"}");
            } else if ("POST".equalsIgnoreCase(method)) {
                Matcher mu = usageUpdate.matcher(path);
                Matcher md = disconnect.matcher(path);
                if (mu.matches()) {
                    Map<String,String> form = readForm(ex);
                    String add = form.get("additionalDataGB");
                    try {
                        double gb = Double.parseDouble(add);
                        hotspotService.updateDeviceUsage(mu.group(1), gb);
                        ok(ex, msg("updated"));
                    } catch (Exception e) { badRequest(ex, msg("invalid additionalDataGB")); }
                } else if (md.matches()) {
                    Map<String,String> form = readForm(ex);
                    String total = form.get("finalDataUsedGB");
                    try {
                        double gb = Double.parseDouble(total);
                        hotspotService.disconnectDevice(md.group(1), gb);
                        ok(ex, msg("disconnected"));
                    } catch (Exception e) { badRequest(ex, msg("invalid finalDataUsedGB")); }
                } else { methodNotAllowed(ex); }
            } else if ("GET".equalsIgnoreCase(method)) {
                Matcher mh = customerHistory.matcher(path);
                Matcher ma = customerActive.matcher(path);
                Matcher maa = allActive.matcher(path);
                if (mh.matches()) {
                    var list = hotspotService.getDeviceHistory(mh.group(1));
                    ok(ex, deviceListToJson(list));
                } else if (ma.matches()) {
                    var list = hotspotService.getActiveDevices(ma.group(1));
                    ok(ex, deviceListToJson(list));
                } else if (maa.matches()) {
                    var list = hotspotService.getAllActiveDevices();
                    ok(ex, deviceListToJson(list));
                } else { badRequest(ex, msg("invalid path")); }
            } else methodNotAllowed(ex);
        }
    }

    private class BillingHandler implements HttpHandler {
        private final Pattern byCustomer = Pattern.compile("^/api/billing/customer/([^/]+)/summary$");
        private final Pattern invoiceByCustomer = Pattern.compile("^/api/billing/customer/([^/]+)/invoice$");
        @Override public void handle(HttpExchange ex) throws IOException {
            setJson(ex);
            String method = ex.getRequestMethod();
            String path = ex.getRequestURI().getPath();
            if ("POST".equalsIgnoreCase(method) && "/api/billing/run".equals(path)) {
                billingService.runBillingCycle();
                ok(ex, msg("billing run"));
            } else if ("GET".equalsIgnoreCase(method)) {
                Matcher m = byCustomer.matcher(path);
                if (m.matches()) {
                    String customerId = m.group(1);
                    ok(ex, json("summary", billingService.getBillingSummary(customerId)));
                } else {
                    Matcher inv = invoiceByCustomer.matcher(path);
                    if (inv.matches()) {
                        String customerId = inv.group(1);
                        byte[] pdf = generateInvoicePdf(customerId);
                        if (pdf == null) { notFound(ex, msg("customer not found")); return; }
                        Headers h = ex.getResponseHeaders();
                        h.set("Content-Type", "application/pdf");
                        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
                        h.set("Content-Disposition", "attachment; filename=invoice-"+customerId+"-"+ts+".pdf");
                        ex.sendResponseHeaders(200, pdf.length);
                        try (OutputStream os = ex.getResponseBody()) { os.write(pdf); }
                    } else badRequest(ex, msg("invalid path"));
                }
            } else methodNotAllowed(ex);
        }
    }

    // Helpers
    private static void setJson(HttpExchange ex) {
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "application/json; charset=utf-8");
        h.set("Access-Control-Allow-Origin", "*");
        h.set("Access-Control-Allow-Methods", "GET,POST,PATCH,DELETE,OPTIONS");
        h.set("Access-Control-Allow-Headers", "Content-Type");
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            try { ex.sendResponseHeaders(204, -1); } catch (IOException ignored) {}
        }
    }

    private static void ok(HttpExchange ex, String body) throws IOException { send(ex, 200, body); }
    private static void created(HttpExchange ex, String body) throws IOException { send(ex, 201, body); }
    private static void badRequest(HttpExchange ex, String body) throws IOException { send(ex, 400, body); }
    private static void notFound(HttpExchange ex, String body) throws IOException { send(ex, 404, body); }
    private static void methodNotAllowed(HttpExchange ex) throws IOException { send(ex, 405, msg("method not allowed")); }

    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static void sendHtml(HttpExchange ex, int status, String html) throws IOException {
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "text/html; charset=utf-8");
        send(ex, status, html);
    }

    private static String contentTypeFromPath(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.endsWith(".html")) return "text/html; charset=utf-8";
        if (p.endsWith(".css")) return "text/css; charset=utf-8";
        if (p.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".ico")) return "image/x-icon";
        if (p.endsWith(".json")) return "application/json; charset=utf-8";
        return "text/plain; charset=utf-8";
    }

    private static Map<String,String> readForm(HttpExchange ex) throws IOException {
        String body;
        try (InputStream is = ex.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        Map<String,String> map = new HashMap<>();
        if (!body.isEmpty()) {
            for (String pair : body.split("&")) {
                String[] kv = pair.split("=", 2);
                String k = urlDecode(kv[0]);
                String v = kv.length>1 ? urlDecode(kv[1]) : "";
                map.put(k, v);
            }
        }
        return map;
    }

    private static String urlDecode(String s) { return URLDecoder.decode(s, StandardCharsets.UTF_8); }
    private static String isJson(String s) { return s == null ? "null" : ("\""+escape(s)+"\""); }
    private static String json(String k, String v) { return "{\""+k+"\":"+isJson(v)+"}"; }
    private static String msg(String m) { return json("message", m); }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }

    private static String customerToJson(Customer c) {
        return "{\"id\":\""+c.getId()+"\",\"name\":\""+escape(c.getName())+"\",\"email\":\""+escape(c.getEmail())+"\"}";
    }

    private static String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i=0;i<list.size();i++) {
            if (i>0) sb.append(',');
            sb.append("\"").append(escape(String.valueOf(list.get(i)))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String deviceListToJson(List<DeviceConnection> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i=0;i<list.size();i++) {
            DeviceConnection d = list.get(i);
            if (i>0) sb.append(',');
            sb.append("{\"id\":\"").append(d.getId()).append("\",")
              .append("\"customerId\":\"").append(d.getCustomerId()).append("\",")
              .append("\"deviceName\":\"").append(escape(d.getDeviceName())).append("\",")
              .append("\"macAddress\":\"").append(escape(d.getMacAddress())).append("\",")
              .append("\"dataUsedGB\":").append(String.format(Locale.ROOT, "%.3f", d.getDataUsedGB())).append(',')
              .append("\"active\":").append(d.isActive()).append(',')
              .append("\"connectedMinutes\":").append(d.getConnectionDurationMinutes())
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private byte[] generateInvoicePdf(String customerId) {
        Optional<Customer> oc = customerService.findById(customerId);
        if (oc.isEmpty()) return null;
        Customer c = oc.get();
        String summary = billingService.getBillingSummary(customerId);
        double total = billingService.calculateBillForCustomer(customerId);
        String issued = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;

                cs.setNonStrokingColor(33, 150, 243); // blue bar
                cs.addRect(0, y - 20, page.getMediaBox().getWidth(), 20);
                cs.fill();
                y -= 40;

                cs.beginText();
                cs.setNonStrokingColor(0);
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(margin, y);
                cs.showText("Invoice");
                cs.endText();

                y -= 24;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Issued: " + issued);
                cs.endText();

                y -= 28;
                // Customer block
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("Bill To:");
                cs.endText();

                y -= 16;
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText(c.getName() + "  <" + c.getEmail() + ">");
                cs.endText();

                y -= 32;
                // Summary line
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText(summary);
                cs.endText();

                y -= 32;
                // Total
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.newLineAtOffset(margin, y);
                cs.showText(String.format(Locale.ROOT, "Amount Due: $%.2f", total));
                cs.endText();
            }
            doc.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
