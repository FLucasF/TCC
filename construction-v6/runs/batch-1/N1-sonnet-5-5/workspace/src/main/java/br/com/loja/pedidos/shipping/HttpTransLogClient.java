package br.com.loja.pedidos.shipping;

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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import br.com.loja.pedidos.common.DomainException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class HttpTransLogClient implements TransLogClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final String baseUrl;
    private final JsonMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public HttpTransLogClient(@Value("${translog.url}") String baseUrl, JsonMapper mapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.mapper = mapper;
    }

    @Override
    public Optional<TransLogQuote> quote(String zipCode, long weightGrams, String service) {
        URI uri = URI.create(baseUrl + "/v2/quote?zip=" + encode(zipCode) + "&weight_g=" + weightGrams
                + "&service=" + encode(service));
        CompletableFuture<HttpResponse<String>> call = null;
        try {
            call = http.sendAsync(HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> response = call.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (response.statusCode() != 200) {
                throw down();
            }
            return parse(mapper.readTree(response.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw down();
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            if (call != null) {
                call.cancel(true);
            }
            throw down();
        }
    }

    private Optional<TransLogQuote> parse(JsonNode body) {
        if (body.isObject() && "ZIP_NOT_SERVED".equals(body.path("error").asString(null))) {
            return Optional.empty();
        }
        JsonNode price = body.path("price_cents");
        JsonNode eta = body.path("eta_hours");
        if (!price.isIntegralNumber() || !eta.isIntegralNumber() || !price.canConvertToInt()
                || !eta.canConvertToInt() || price.asInt() < 0 || eta.asInt() < 0) {
            throw down();
        }
        return Optional.of(new TransLogQuote(price.asInt(), eta.asInt()));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static DomainException down() {
        return new DomainException("CARRIER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
