package reference.suite;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import reference.TransLog;

/**
 * The fake TransLog the agent's service talks to during the suite.
 *
 * It answers whatever the current test case says ({@link #answerWith}) and records every call, so the
 * suite can check the service asked for the right zip code, weight and service code. The same object
 * also plays the TransLog for the reference model ({@link #quote}), so the model and the service always
 * see the same answer.
 */
public final class FakeTransLog implements TransLog {

    /** One call the agent's service made: {@code GET /v2/quote?zip=…&weight_g=…&service=…}. */
    public record Call(String path, String zip, String weightGrams, String service) {

        /** Same zip, same weight (as a number) and same service code. */
        public boolean sameQueryAs(Call other) {
            return java.util.Objects.equals(zip, other.zip) && java.util.Objects.equals(service, other.service)
                    && grams(weightGrams) == grams(other.weightGrams);
        }

        private static double grams(String text) {
            try { return Double.parseDouble(text); } catch (Exception e) { return Double.NaN; }
        }
    }

    private final HttpServer server;
    private volatile Answer currentAnswer = new Quote(1, 1);
    private final List<Call> serviceCalls = new ArrayList<>();
    private volatile Call lastModelCall;

    public FakeTransLog(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    public void answerWith(Answer answer) {
        currentAnswer = answer == null ? new Quote(1, 1) : answer;
    }

    public synchronized void clearCalls() {
        serviceCalls.clear();
        lastModelCall = null;
    }

    /** What the reference model asked for (null if it did not need the TransLog). */
    public Call lastModelCall() {
        return lastModelCall;
    }

    public synchronized List<Call> serviceCalls() {
        return List.copyOf(serviceCalls);
    }

    public void stop() {
        server.stop(0);
    }

    /** The reference model asks here, and gets the same answer the service gets over HTTP. */
    @Override
    public Answer quote(String zipCode, int weightGrams, String service) {
        lastModelCall = new Call("/v2/quote", zipCode, String.valueOf(weightGrams), service);
        return currentAnswer;
    }

    private void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        Map<String, String> query = parseQuery(uri.getRawQuery());
        synchronized (this) {
            serviceCalls.add(new Call(uri.getPath(), query.get("zip"), query.get("weight_g"), query.get("service")));
        }
        switch (currentAnswer) {
            case Quote q -> reply(exchange, 200, "{\"price_cents\":" + q.priceCents() + ",\"eta_hours\":" + q.etaHours() + "}");
            case ZipNotServed z -> reply(exchange, 200, "{\"error\":\"ZIP_NOT_SERVED\"}");
            case Unavailable u -> reply(exchange, 500, "internal error");
        }
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null) return params;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) params.put(pair.substring(0, eq), java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return params;
    }
}
