package br.com.loja.shipping;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class TransLogGateway implements CarrierGateway {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final JsonMapper json = JsonMapper.builder().build();

    public TransLogGateway(@Value("${translog.url:}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public CarrierQuote quote(String zipCode, long weightGrams, String service) {
        JsonNode body;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                            baseUrl + "/v2/quote?zip=" + zipCode + "&weight_g=" + weightGrams + "&service="
                                    + (service.equals("STD") ? "STD" : "EXP")))
                    .timeout(TIMEOUT).GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new CarrierDownException("TransLog respondeu " + response.statusCode(), null);
            }
            body = json.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CarrierDownException("Consulta à TransLog interrompida", e);
        } catch (CarrierDownException e) {
            throw e;
        } catch (Exception e) {
            throw new CarrierDownException("Falha ao consultar a TransLog", e);
        }
        if (body.has("error")) {
            if ("ZIP_NOT_SERVED".equals(body.get("error").asString())) {
                throw new ZipNotServedException();
            }
            throw new CarrierDownException("TransLog respondeu erro " + body.get("error"), null);
        }
        JsonNode cents = body.get("price_cents");
        JsonNode hours = body.get("eta_hours");
        if (cents == null || hours == null || !cents.isIntegralNumber() || !hours.isIntegralNumber()
                || hours.longValue() < 0) {
            throw new CarrierDownException("Resposta inesperada da TransLog", null);
        }
        BigDecimal price = BigDecimal.valueOf(cents.longValue(), 2);
        int days = (int) ((hours.longValue() + 23) / 24);
        return new CarrierQuote(price, days);
    }
}
