package com.logistics.api;

import java.util.*;
import java.io.*;
import java.net.*;

public class RESTAPI {
    private int port;
    private ServerSocket serverSocket;
    private boolean running;
    private Map<String, RequestHandler> handlers;
    public interface RequestHandler {
        String handle(Map<String, String> params);
    }
    public static class Request {
        private String method;
        private String path;
        private Map<String, String> params;
        private String body;
        public Request(String method, String path, Map<String, String> params, String body) {
            this.method = method;
            this.path = path;
            this.params = params;
            this.body = body;
        }
        public String getMethod() { return method; }
        public String getPath() { return path; }
        public Map<String, String> getParams() { return params; }
        public String getBody() { return body; }
    }
    
    public static class Response {
        private int statusCode;
        private String contentType;
        private String body;
        public Response(int statusCode, String contentType, String body) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.body = body;
        }
        public String toHTTPString() {
            StringBuilder sb = new StringBuilder();
            sb.append("HTTP/1.1 ").append(statusCode).append(" OK\r\n");
            sb.append("Content-Type: ").append(contentType).append("\r\n");
            sb.append("Content-Length: ").append(body.length()).append("\r\n");
            sb.append("Connection: close\r\n");
            sb.append("\r\n");
            sb.append(body);
            return sb.toString();
        }
    }
    public RESTAPI(int port) {
        this.port = port;
        this.handlers = new HashMap<>();
        this.running = false;
        registerDefaultHandlers();
    }
    private void registerDefaultHandlers() {
        handlers.put("GET /health", params -> {
            return "{\"status\": \"ok\", \"timestamp\": " + System.currentTimeMillis() + "}";
        });
        handlers.put("GET /status", params -> {
            return "{\"agents\": 5, \"orders\": 10, \"resources\": 3}";
        });
        handlers.put("GET /algorithms", params -> {
            return "{\"algorithms\": [\"MCAA\", \"GA\", \"SA\", \"PSO\"]}";
        });
        handlers.put("POST /simulation/run", params -> {
            int orders = Integer.parseInt(params.getOrDefault("orders", "10"));
            int resources = Integer.parseInt(params.getOrDefault("resources", "3"));
            return "{\"status\": \"started\", \"orders\": " + orders + ", \"resources\": " + resources + "}";
        });
        handlers.put("GET /kpis", params -> {
            return "{\"avg_delivery_time\": 24.5, \"avg_cost\": 5000, \"escalations\": 2}";
        });
    }
    
    public void registerHandler(String method, String path, RequestHandler handler) {
        handlers.put(method + " " + path, handler);
    }
    
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("REST API started on port " + port);
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                handleClient(clientSocket);
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error handling client: " + e.getMessage());
                }
            }
        }
    }
    
    private void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream())) {
            String requestLine = in.readLine();
            if (requestLine == null) return;
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;
            String method = parts[0];
            String path = parts[1];
            
            Map<String, String> params = new HashMap<>();
            if (path.contains("?")) {
                String[] pathParts = path.split("\\?");
                path = pathParts[0];
                if (pathParts.length > 1) {
                    String queryString = pathParts[1];
                    for (String param : queryString.split("&")) {
                        String[] keyValue = param.split("=");
                        if (keyValue.length == 2) {
                            params.put(keyValue[0], URLDecoder.decode(keyValue[1], "UTF-8"));
                        }
                    }
                }
            }
            
            
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                
            }
            
            String handlerKey = method + " " + path;
            RequestHandler handler = handlers.get(handlerKey);
            Response response;
            if (handler != null) {
                String body = handler.handle(params);
                response = new Response(200, "application/json", body);
            } else {
                response = new Response(404, "application/json", "{\"error\": \"Not found\"}");
            }
            out.print(response.toHTTPString());
            out.flush();
        } catch (IOException e) {
            System.err.println("Error handling request: " + e.getMessage());
        }
    }
    
     
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }
    
     
    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        RESTAPI api = new RESTAPI(port);
        api.start();
    }
}