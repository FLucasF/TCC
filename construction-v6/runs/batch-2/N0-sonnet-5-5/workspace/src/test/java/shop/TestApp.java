package shop;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.Javalin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

/** Servidor real em porta livre, armazenamento em memória e transportadora de mentira. */
class TestApp implements AutoCloseable {
    record Reply(int status, JsonNode body) {}

    private static final com.fasterxml.jackson.databind.ObjectMapper READER = new com.fasterxml.jackson.databind.ObjectMapper()
            .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .configure(com.fasterxml.jackson.databind.cfg.JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false);

    volatile Optional<CarrierQuote> carrierAnswer = Optional.empty();
    volatile boolean carrierDown = false;
    final MemoryStore store = new MemoryStore();
    final Javalin app;
    private final HttpClient http = HttpClient.newHttpClient();

    TestApp(Instant now) {
        CarrierGateway gateway = (zip, grams, service) -> {
            if (carrierDown) throw new CarrierDownException("down", null);
            return carrierAnswer;
        };
        app = Api.create(store, gateway, Clock.fixed(now, ZoneOffset.UTC)).start(0);
    }

    void carrier(int cents, int hours) {
        carrierAnswer = Optional.of(new CarrierQuote(java.math.BigDecimal.valueOf(cents, 2), hours));
    }

    Reply post(String path, String body) {
        return send(HttpRequest.newBuilder(uri(path)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body == null ? "" : body)).build());
    }

    Reply get(String path) {
        return send(HttpRequest.newBuilder(uri(path)).GET().build());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + app.port() + path);
    }

    private Reply send(HttpRequest request) {
        try {
            HttpResponse<String> r = http.send(request, HttpResponse.BodyHandlers.ofString());
            return new Reply(r.statusCode(), READER.readTree(r.body()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        app.stop();
    }
}
