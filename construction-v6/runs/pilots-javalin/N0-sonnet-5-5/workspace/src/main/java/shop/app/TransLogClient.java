package shop.app;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import shop.domain.CarrierGateway;
import shop.domain.Json;

/** Quotes freight on the TransLog website. Anything unexpected, or slower than 2 seconds, means "down". */
public final class TransLogClient implements CarrierGateway {
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public TransLogClient(String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
    }

    @Override
    public Optional<Quote> quote(String zipCode, int weightGrams, String service) {
        CompletableFuture<HttpResponse<String>> future = null;
        try {
            URI uri = URI.create(baseUrl + "/v2/quote?zip=" + URLEncoder.encode(zipCode, StandardCharsets.UTF_8)
                    + "&weight_g=" + weightGrams + "&service=" + URLEncoder.encode(service, StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();
            future = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> response = future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (response.statusCode() != 200) {
                throw new UnavailableException("TransLog answered " + response.statusCode(), null);
            }
            return parse(Json.MAPPER.readTree(response.body()));
        } catch (UnavailableException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnavailableException("interrupted", e);
        } catch (Exception e) {
            throw new UnavailableException("TransLog failed", e);
        } finally {
            if (future != null) {
                future.cancel(true);
            }
        }
    }

    private static Optional<Quote> parse(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new UnavailableException("unexpected TransLog body", null);
        }
        if (body.has("error")) {
            if (body.get("error").isTextual() && "ZIP_NOT_SERVED".equals(body.get("error").asText())) {
                return Optional.empty();
            }
            throw new UnavailableException("TransLog error " + body.get("error"), null);
        }
        JsonNode cents = body.path("price_cents");
        JsonNode hours = body.path("eta_hours");
        if (!cents.isIntegralNumber() || !hours.isIntegralNumber() || cents.asLong() < 0 || hours.asLong() < 0
                || !hours.canConvertToInt()) {
            throw new UnavailableException("unexpected TransLog quote", null);
        }
        BigDecimal cost = BigDecimal.valueOf(cents.asLong(), 2);
        int days = (int) ((hours.asLong() + 23) / 24);
        return Optional.of(new Quote(cost, days));
    }
}
