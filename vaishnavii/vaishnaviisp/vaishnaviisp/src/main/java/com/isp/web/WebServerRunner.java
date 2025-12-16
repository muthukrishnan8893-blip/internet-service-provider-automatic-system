package com.isp.web;

public class WebServerRunner {
    public static void main(String[] args) throws Exception {
        int port = 8081; // default alternate port
        if (args != null && args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        } else {
            String env = System.getenv("PORT");
            if (env != null) { try { port = Integer.parseInt(env); } catch (NumberFormatException ignored) {} }
        }
        WebServer server = new WebServer();
        server.start(port);
        System.out.println("Web server started on http://localhost:" + port);
    }
}
