package com.loja.orders.support;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpServer;

                                                                  
public final class FakeTranslog {

    private static final FakeTranslog INSTANCE = new FakeTranslog();

    private final HttpServer server;
    private final List<String> queries = new CopyOnWriteArrayList<>();
    private volatile int status;
    private volatile String body;
    private volatile long delayMillis;

    private FakeTranslog() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/v2/quote", exchange -> {
            queries.add(exchange.getRequestURI().getQuery());
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            } catch (IOException ignored) {
                                                
            }
        });
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        reset();
    }

    public static FakeTranslog get() {
        return INSTANCE;
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void reset() {
        respond(200, "{ \"price_cents\": 2000, \"eta_hours\": 24 }");
        delayMillis = 0;
        queries.clear();
    }

    public void quote(int priceCents, int etaHours) {
        respond(200, "{ \"price_cents\": " + priceCents + ", \"eta_hours\": " + etaHours + " }");
    }

    public void respond(int status, String body) {
        this.status = status;
        this.body = body;
    }

    public void delay(long millis) {
        this.delayMillis = millis;
    }

    public List<String> queries() {
        return queries;
    }
}
