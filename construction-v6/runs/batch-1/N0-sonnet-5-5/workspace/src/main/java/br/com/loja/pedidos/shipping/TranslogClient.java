package br.com.loja.pedidos.shipping;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class TranslogClient implements CarrierGateway {

    private static final Logger log = LoggerFactory.getLogger(TranslogClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final JsonMapper mapper = JsonMapper.builder().build();

    public TranslogClient(@Value("${translog.url:}") String baseUrl) {
        String url = baseUrl == null ? "" : baseUrl.trim();
        this.baseUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @Override
    public CarrierResult quote(String zipCode, long weightGrams, ServiceLevel level) {
        if (baseUrl.isEmpty()) {
            log.warn("TRANSLOG_URL is not set");
            return new Unavailable();
        }
        CompletableFuture<HttpResponse<String>> pending = null;
        try {
            String service = level == ServiceLevel.EXPRESS ? "EXP" : "STD";
            URI uri = URI.create(baseUrl + "/v2/quote?zip=" + URLEncoder.encode(zipCode, StandardCharsets.UTF_8)
                    + "&weight_g=" + weightGrams + "&service=" + service);
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();
            pending = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> response = pending.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (response.statusCode() != 200) {
                return new Unavailable();
            }
            return parse(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Unavailable();
        } catch (RuntimeException | java.util.concurrent.ExecutionException
                | java.util.concurrent.TimeoutException e) {
            log.warn("TransLog quote failed: {}", e.toString());
            return new Unavailable();
        } finally {
            if (pending != null && !pending.isDone()) {
                pending.cancel(true);
            }
        }
    }

    private CarrierResult parse(String body) {
        Map<?, ?> json = mapper.readValue(body, Map.class);
        Object error = json.get("error");
        if (error != null) {
            return "ZIP_NOT_SERVED".equals(error) ? new NotServed() : new Unavailable();
        }
        if (json.get("price_cents") instanceof Number cents && json.get("eta_hours") instanceof Number eta
                && isIntegral(cents) && isIntegral(eta) && cents.longValue() >= 0 && eta.longValue() >= 0) {
            return new Quoted(BigDecimal.valueOf(cents.longValue(), 2), eta.longValue());
        }
        return new Unavailable();
    }

    private static boolean isIntegral(Number n) {
        return n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte;
    }
}
