package com.isp.web;

import com.isp.model.Notification;
import com.isp.model.NotificationPreferences;
import com.isp.service.NotificationService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP handler for notification-related API endpoints
 */
public class NotificationHandler implements HttpHandler {
    private final NotificationService notificationService;
    private final Map<String, String> sessions;
    private final Gson gson;

    public NotificationHandler(NotificationService notificationService, Map<String, String> sessions) {
        this.notificationService = notificationService;
        this.sessions = sessions;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        
        // Enable CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        if ("OPTIONS".equals(method)) {
            sendResponse(exchange, 200, "");
            return;
        }

        try {
            if (path.equals("/api/notifications/list") && "GET".equals(method)) {
                handleGetNotifications(exchange);
            } else if (path.equals("/api/notifications/unread") && "GET".equals(method)) {
                handleGetUnreadNotifications(exchange);
            } else if (path.equals("/api/notifications/count") && "GET".equals(method)) {
                handleGetUnreadCount(exchange);
            } else if (path.equals("/api/notifications/mark-read") && "POST".equals(method)) {
                handleMarkAsRead(exchange);
            } else if (path.equals("/api/notifications/mark-all-read") && "POST".equals(method)) {
                handleMarkAllAsRead(exchange);
            } else if (path.equals("/api/notifications/preferences") && "GET".equals(method)) {
                handleGetPreferences(exchange);
            } else if (path.equals("/api/notifications/preferences") && "POST".equals(method)) {
                handleUpdatePreferences(exchange);
            } else if (path.equals("/api/notifications/test") && "POST".equals(method)) {
                handleTestNotification(exchange);
            } else {
                sendJsonResponse(exchange, 404, Map.of("status", "error", "message", "Not found"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendJsonResponse(exchange, 500, Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private void handleGetNotifications(HttpExchange exchange) throws IOException {
        String userId = getUserIdFromToken(exchange);
        if (userId == null) {
            sendJsonResponse(exchange, 401, Map.of("status", "error", "message", "Unauthorized"));
            return;
        }

        Map<String, String> params = parseQueryString(exchange.getRequestURI().getQuery());
        int limit = Integer.parseInt(params.getOrDefault("limit", "50"));

        List<Notification> notifications = notificationService.getUserNotifications(userId, limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("notifications", notifications);
        response.put("count", notifications.size());
        
        sendJsonResponse(exchange, 200, response);
    }

    private void handleGetUnreadNotifications(HttpExchange exchange) throws IOException {
        String userId = getUserIdFromToken(exchange);
        if (userId == null) {
            sendJsonResponse(exchange, 401, Map.of("status", "error", "message", "Unauthorized"));
            return;
        }

        List<Notification> notifications = notificationService.getUnreadNotifications(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("notifications", notifications);
        response.put("count", notifications.size());
        
        sendJsonResponse(exchange, 200, response);
    }

    private void handleGetUnreadCount(HttpExchange exchange) throws IOException {
        String userId = getUserIdFromToken(exchange);
        if (userId == null) {
            sendJsonResponse(exchange, 401, Map.of("status", "error", "message", "Unauthorized"));
            return;
        }

        int count = notificationService.getUnreadCount(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("unreadCount", count);
        
        sendJsonResponse(exchange, 200, response);
    }

    private void handleMarkAsRead(HttpExchange exchange) throws IOException {
        String userId = getUserIdFromToken(exchange);
        if (userId == null) {
            sendJsonResponse(exchange, 401, Map.of("status", "error", "message", "Unauthorized"));
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject json = gson.fromJson(body, JsonObject.class);
        String notificationId = json.get("notificationId").getAsString();

        notificationService.markAsRead(notificationId);
        
        sendJsonResponse(exchange, 200, Map.of("status", "success", "message", "Notification marked as read"));
    }

    private void handleMarkAllAsRead(HttpExchange exchange) throws IOException {
        String userId = getUserIdFromToken(exchange);
        if (userId == null) {
            sendJsonResponse(exchange, 401, Map.of("status", "error", "message", "Unauthorized"));
            return;
        }

        notificationService.markAllAsRead(userId);
        
        sendJsonResponse(exchange, 200, Map.of("status", "success", "message", "All notifications marked as read"));
    }

    private void handleGetPreferences(HttpExchange exchange) throws IOException {
        String userId = getUserIdFromToken(exchange);
        if (userId == null) {
            sendJsonResponse(exchange, 401, Map.of("status", "error", "message", "Unauthorized"));
            return;
        }

        NotificationPreferences prefs = notificationService.getPreferences(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("preferences", prefs);
        
        sendJsonResponse(exchange, 200, response);
    }

    private void handleUpdatePreferences(HttpExchange exchange) throws IOException {
        String userId = getUserIdFromToken(exchange);
        if (userId == null) {
            sendJsonResponse(exchange, 401, Map.of("status", "error", "message", "Unauthorized"));
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        NotificationPreferences prefs = gson.fromJson(body, NotificationPreferences.class);
        prefs.setUserId(userId);

        notificationService.updatePreferences(prefs);
        
        sendJsonResponse(exchange, 200, Map.of("status", "success", "message", "Preferences updated successfully"));
    }

    private void handleTestNotification(HttpExchange exchange) throws IOException {
        String userId = getUserIdFromToken(exchange);
        if (userId == null) {
            sendJsonResponse(exchange, 401, Map.of("status", "error", "message", "Unauthorized"));
            return;
        }

        // Send a test notification
        notificationService.sendNotification(
            userId,
            "SYSTEM",
            "Test Notification",
            "This is a test notification to verify your notification settings are working correctly.",
            "LOW"
        );
        
        sendJsonResponse(exchange, 200, Map.of("status", "success", "message", "Test notification sent"));
    }

    private String getUserIdFromToken(HttpExchange exchange) {
        Map<String, String> params = parseQueryString(exchange.getRequestURI().getQuery());
        String token = params.get("token");
        
        if (token == null) {
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }
        
        return token != null ? sessions.get(token) : null;
    }

    private Map<String, String> parseQueryString(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        
        for (String param : query.split("&")) {
            String[] parts = param.split("=", 2);
            if (parts.length == 2) {
                try {
                    result.put(
                        URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    );
                } catch (Exception e) {
                    // Skip invalid params
                }
            }
        }
        return result;
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
