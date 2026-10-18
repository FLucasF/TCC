package shop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

class TransLogGateway implements CarrierGateway {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long TIMEOUT_MS = 2000;

    private final String baseUrl;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(TIMEOUT_MS)).build();

    TransLogGateway(String baseUrl) {
        this.baseUrl = baseUrl == null ? null : baseUrl.replaceAll("/+$", "");
    }

    @Override
    public Optional<CarrierQuote> quote(String zipCode, long weightGrams, String service) {
        if (baseUrl == null || baseUrl.isBlank()) throw new CarrierDownException("TRANSLOG_URL not set", null);
        CompletableFuture<HttpResponse<String>> future = null;
        try {
            URI uri = URI.create(baseUrl + "/v2/quote?zip=" + zipCode + "&weight_g=" + weightGrams + "&service=" + service);
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
            future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> response = future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (response.statusCode() != 200) throw new CarrierDownException("status " + response.statusCode(), null);
            return parse(response.body());
        } catch (CarrierDownException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CarrierDownException("interrupted", e);
        } catch (Exception e) {
            throw new CarrierDownException("TransLog failure", e);
        } finally {
            if (future != null) future.cancel(true);
        }
    }

    private static Optional<CarrierQuote> parse(String body) throws Exception {
        JsonNode json = MAPPER.readTree(body);
        if (json == null || !json.isObject()) throw new CarrierDownException("unexpected body", null);
        JsonNode error = json.get("error");
        if (error != null) {
            if (error.isTextual() && error.asText().equals("ZIP_NOT_SERVED")) return Optional.empty();
            throw new CarrierDownException("carrier error", null);
        }
        JsonNode price = json.get("price_cents");
        JsonNode eta = json.get("eta_hours");
        if (price == null || eta == null || !price.isIntegralNumber() || !eta.isIntegralNumber()
                || !price.canConvertToLong() || !eta.canConvertToInt() || price.asLong() < 0 || eta.asInt() < 0) {
            throw new CarrierDownException("unexpected body", null);
        }
        return Optional.of(new CarrierQuote(BigDecimal.valueOf(price.asLong(), 2), eta.asInt()));
    }
}
