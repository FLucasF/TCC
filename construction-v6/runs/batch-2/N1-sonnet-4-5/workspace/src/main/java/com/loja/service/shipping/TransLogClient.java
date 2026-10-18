package com.loja.service.shipping;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.domain.Region;
import com.loja.domain.ShippingMethod;
import com.loja.util.MoneyUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TransLogClient {
    private final String transLogUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TransLogClient(String transLogUrl) {
        this.transLogUrl = transLogUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    public ShippingResult quote(String zipCode, BigDecimal weightKg, ShippingMethod method, Region region) {
        String service = method == ShippingMethod.ECONOMY ? "STD" : "EXP";
        int weightGrams = weightKg.multiply(new BigDecimal("1000")).intValue();

        String url = String.format("%s/v2/quote?zip=%s&weight_g=%d&service=%s",
            transLogUrl, zipCode, weightGrams, service);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());

                if (json.has("error")) {
                    return null;
                }

                int priceCents = json.get("price_cents").asInt();
                int etaHours = json.get("eta_hours").asInt();

                BigDecimal price = MoneyUtils.roundMoney(new BigDecimal(priceCents).divide(new BigDecimal("100")));
                int deliveryDays = (int) Math.ceil(etaHours / 24.0);

                int additionalDays = switch (region) {
                    case NORTH -> 3;
                    case NORTHEAST -> 2;
                    default -> 0;
                };

                return new ShippingResult(price, deliveryDays + additionalDays);
            }
        } catch (Exception e) {
        }

        throw new RuntimeException("CARRIER_UNAVAILABLE");
    }
}
