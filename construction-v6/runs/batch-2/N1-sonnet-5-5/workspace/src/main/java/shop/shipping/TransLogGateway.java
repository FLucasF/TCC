package shop.shipping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import shop.ApiException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class TransLogGateway implements CarrierGateway {
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final String baseUrl;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper mapper = new ObjectMapper();

    public TransLogGateway(String baseUrl) {
        this.baseUrl = baseUrl == null ? null : baseUrl.replaceAll("/+$", "");
    }

    @Override
    public CarrierQuote quote(String zipCode, int weightGrams, String service) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw down();
        }
        JsonNode body;
        try {
            URI uri = URI.create(baseUrl + "/v2/quote?zip=" + zipCode + "&weight_g=" + weightGrams + "&service=" + service);
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw down();
            }
            body = mapper.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw down();
        } catch (IOException | RuntimeException e) {
            if (e instanceof ApiException api) {
                throw api;
            }
            throw down();
        }
        if (body == null || !body.isObject()) {
            throw down();
        }
        if (body.has("error")) {
            if ("ZIP_NOT_SERVED".equals(body.get("error").asText())) {
                throw ApiException.unprocessable("SHIPPING_METHOD_UNAVAILABLE");
            }
            throw down();
        }
        JsonNode price = body.get("price_cents");
        JsonNode eta = body.get("eta_hours");
        if (price == null || eta == null || !price.isIntegralNumber() || !eta.isIntegralNumber()
                || !eta.canConvertToInt() || price.asLong() < 0 || eta.asInt() < 0) {
            throw down();
        }
        return new CarrierQuote(BigDecimal.valueOf(price.asLong(), 2), eta.asInt());
    }

    private static ApiException down() {
        return ApiException.unavailable("CARRIER_UNAVAILABLE");
    }
}
