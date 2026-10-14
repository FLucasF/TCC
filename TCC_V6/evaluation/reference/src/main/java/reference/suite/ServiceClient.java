package reference.suite;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

import reference.Purchase;

/**
 * The agent's service seen through its HTTP API: one method per route of the prompt, each returning the
 * status code and the JSON body ({@link Reply}). Nothing here judges anything; {@link Lockstep} does.
 */
public final class ServiceClient {

    /** What came back: the HTTP status and the body (MissingNode when it was not JSON). */
    public record Reply(int status, JsonNode body, String rawBody) {

        public String error() {
            return body.path("error").asText(null);
        }
    }

    static final ObjectMapper JSON = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public ServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // ------------------------------------------------------------------ the routes

    public Reply checkoutSummary(Purchase purchase) { return post("/checkout/summary", purchase); }

    public Reply createOrder(Purchase purchase) { return post("/orders", purchase); }

    public Reply getOrder(String id) { return get("/orders/" + id); }

    public Reply pay(String id, String transactionId, Instant paidAt) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (transactionId != null) body.put("transactionId", transactionId);
        if (paidAt != null) body.put("paidAt", paidAt.toString());
        return post("/orders/" + id + "/payment", body);
    }

    public Reply ship(String id, String trackingCode) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (trackingCode != null) body.put("trackingCode", trackingCode);
        return post("/orders/" + id + "/shipment", body);
    }

    public Reply deliver(String id) { return post("/orders/" + id + "/delivery", Map.of()); }

    public Reply cancel(String id) { return post("/orders/" + id + "/cancellation", Map.of()); }

    public Reply getProduct(String sku) { return get("/products/" + sku); }

    public Reply getCustomer(String cpf) { return get("/customers/" + cpf); }

    public Reply getNotifications(String cpf) { return get("/customers/" + cpf + "/notifications"); }

    public Reply markAsRead(String notificationId) { return post("/notifications/" + notificationId + "/read", Map.of()); }

    // ------------------------------------------------------------------ plain HTTP

    private Reply get(String path) {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET());
    }

    private Reply post(String path, Object body) {
        try {
            String json = JSON.writeValueAsString(body);
            return send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json)));
        } catch (Exception e) {
            throw new IllegalStateException("could not write the request body", e);
        }
    }

    private Reply send(HttpRequest.Builder request) {
        try {
            HttpResponse<String> response = http.send(request.timeout(Duration.ofSeconds(30)).build(), HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), parse(response.body()), response.body());
        } catch (Exception e) {
            return new Reply(-1, MissingNode.getInstance(), "no answer: " + e);   // -1 = the service did not answer
        }
    }

    private static JsonNode parse(String text) {
        try {
            return text == null || text.isBlank() ? MissingNode.getInstance() : JSON.readTree(text);
        } catch (Exception notJson) {
            return MissingNode.getInstance();
        }
    }
}
