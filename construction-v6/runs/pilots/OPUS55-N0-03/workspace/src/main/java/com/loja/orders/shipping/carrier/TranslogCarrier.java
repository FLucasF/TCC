package com.loja.orders.shipping.carrier;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Cotação de frete na TransLog ({@code GET {TRANSLOG_URL}/v2/quote}). */
@Component
public class TranslogCarrier implements Carrier {

    private static final String ZIP_NOT_SERVED = "ZIP_NOT_SERVED";

    private final String baseUrl;
    private final Duration timeout;
    private final JsonMapper mapper;
    private final HttpClient http;

    public TranslogCarrier(@Value("${translog.url:}") String baseUrl,
            @Value("${translog.timeout:2s}") Duration timeout, JsonMapper mapper) {
        this.baseUrl = baseUrl.strip().replaceAll("/+$", "");
        this.timeout = timeout;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(timeout).build();
    }

    @Override
    public CarrierQuote quote(String zipCode, int weightGrams, CarrierService service) {
        if (baseUrl.isEmpty()) {
            throw new CarrierUnavailableException("TRANSLOG_URL não configurada");
        }
        URI uri;
        try {
            uri = URI.create(baseUrl + "/v2/quote"
                    + "?zip=" + URLEncoder.encode(zipCode, StandardCharsets.UTF_8)
                    + "&weight_g=" + weightGrams
                    + "&service=" + serviceCode(service));
        } catch (IllegalArgumentException e) {
            throw new CarrierUnavailableException("TRANSLOG_URL inválida", e);
        }
        HttpResponse<String> response = get(uri);
        if (response.statusCode() != 200) {
            throw new CarrierUnavailableException("TransLog respondeu " + response.statusCode());
        }
        return parse(response.body());
    }

    private HttpResponse<String> get(URI uri) {
        CompletableFuture<HttpResponse<String>> call;
        try {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).GET().build();
            call = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (IllegalArgumentException e) {
            throw new CarrierUnavailableException("TRANSLOG_URL inválida", e);
        }
        try {
            // O prazo vale para a resposta inteira, não só para a conexão.
            return call.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            call.cancel(true);
            throw new CarrierUnavailableException("TransLog não respondeu a tempo", e);
        } catch (ExecutionException e) {
            throw new CarrierUnavailableException("Falha ao chamar a TransLog", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            call.cancel(true);
            throw new CarrierUnavailableException("Chamada à TransLog interrompida", e);
        }
    }

    private CarrierQuote parse(String body) {
        JsonNode json;
        try {
            json = mapper.readTree(body);
        } catch (JacksonException e) {
            throw new CarrierUnavailableException("Resposta inválida da TransLog", e);
        }
        if (json == null || !json.isObject()) {
            throw new CarrierUnavailableException("Resposta inválida da TransLog");
        }
        if (json.has("error")) {
            JsonNode error = json.get("error");
            if (error.isString() && ZIP_NOT_SERVED.equals(error.stringValue())) {
                throw new ZipNotServedException();
            }
            throw new CarrierUnavailableException("TransLog respondeu erro " + error);
        }
        JsonNode cents = json.path("price_cents");
        JsonNode hours = json.path("eta_hours");
        if (!isNonNegativeInt(cents) || !isNonNegativeInt(hours)) {
            throw new CarrierUnavailableException("Resposta incompleta da TransLog");
        }
        return new CarrierQuote(BigDecimal.valueOf(cents.asLong(), 2), hours.asInt());
    }

    private static boolean isNonNegativeInt(JsonNode node) {
        return node.isNumber() && node.canConvertToExactIntegral() && node.canConvertToInt() && node.asInt() >= 0;
    }

    private static String serviceCode(CarrierService service) {
        return switch (service) {
            case STANDARD -> "STD";
            case EXPRESS -> "EXP";
        };
    }
}
