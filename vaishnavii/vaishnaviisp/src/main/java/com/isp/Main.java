package com.isp;

import com.isp.web.WebServer;

/**
 * Main entry point - starts the ISP Management System web server.
 */
public class Main {
    public static void main(String[] args) {
        int port = 8081;
        if (args != null && args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }

        try {
            WebServer server = new WebServer();
            server.start(port);
            System.out.println("Web server started on http://localhost:" + port);
            // Keep main thread alive so the HttpServer threads keep serving
            synchronized (Main.class) {
                try {
                    Main.class.wait();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}