package com.isp.web;

public class WebServerRunner {
    public static void main(String[] args) throws Exception {
        int port = 8081; // default port
        if (args != null && args.length > 0) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        } else {
            String env = System.getenv("PORT");
            if (env != null) { try { port = Integer.parseInt(env); } catch (NumberFormatException ignored) {} }
        }
        // Use the original web server
        WebServer server = new WebServer();
        server.start(port);
        System.out.println("WebServerRunner: server started on port " + port);
        // Keep the JVM alive indefinitely
        Thread.currentThread().join();
    }
}
