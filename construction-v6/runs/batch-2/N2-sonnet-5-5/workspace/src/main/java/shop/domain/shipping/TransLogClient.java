package shop.domain.shipping;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import shop.Json;

public class TransLogClient implements CarrierClient {
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public TransLogClient(String baseUrl) {
        this.baseUrl = baseUrl == null ? null : baseUrl.replaceAll("/+$", "");
    }

    @Override
    public CarrierQuote quote(String zipCode, long weightGrams, String service) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new CarrierDownException("TRANSLOG_URL not configured", null);
        }
        JsonNode body;
        try {
            URI uri = URI.create(baseUrl + "/v2/quote?zip=" + zipCode + "&weight_g=" + weightGrams
                    + "&service=" + service);
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new CarrierDownException("TransLog status " + response.statusCode(), null);
            }
            body = Json.MAPPER.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CarrierDownException("interrupted", e);
        } catch (CarrierDownException e) {
            throw e;
        } catch (Exception e) {
            throw new CarrierDownException("TransLog failure", e);
        }
        if (body != null && body.isObject() && body.has("error")) {
            if ("ZIP_NOT_SERVED".equals(body.get("error").asText())) {
                throw new ZipNotServedException();
            }
            throw new CarrierDownException("TransLog error " + body.get("error"), null);
        }
        if (body == null || !body.path("price_cents").canConvertToLong() || !body.path("price_cents").isIntegralNumber()
                || !body.path("eta_hours").canConvertToInt() || !body.path("eta_hours").isIntegralNumber()
                || body.path("price_cents").asLong() < 0 || body.path("eta_hours").asInt() < 0) {
            throw new CarrierDownException("TransLog unexpected body", null);
        }
        return new CarrierQuote(body.get("price_cents").asLong(), body.get("eta_hours").asInt());
    }
}
