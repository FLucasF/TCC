package com.shop.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.exception.BusinessException;
import com.shop.model.enums.Region;
import com.shop.model.enums.ShippingMethod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ShippingService {
    private final String translogUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ShippingService(String translogUrl) {
        this.translogUrl = translogUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public static class ShippingQuote {
        public BigDecimal cost;
        public int deliveryDays;

        public ShippingQuote(BigDecimal cost, int deliveryDays) {
            this.cost = cost;
            this.deliveryDays = deliveryDays;
        }
    }

    public ShippingQuote calculateShipping(ShippingMethod method, String zipCode, BigDecimal weightKg, Region region) {
        return switch (method) {
            case ECONOMY -> calculateTranslog(zipCode, weightKg, "STD", region, 7);
            case EXPRESS -> calculateTranslog(zipCode, weightKg, "EXP", region, 0);
            case STORE_PICKUP -> new ShippingQuote(BigDecimal.ZERO, 1);
            case COURIER -> new ShippingQuote(new BigDecimal("18.00"), 0);
        };
    }

    private ShippingQuote calculateTranslog(String zipCode, BigDecimal weightKg, String service, Region region, int baseDays) {
        int weightGrams = weightKg.multiply(new BigDecimal("1000")).intValue();
        String url = String.format("%s/v2/quote?zip=%s&weight_g=%d&service=%s",
                translogUrl, zipCode, weightGrams, service);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new BusinessException("CARRIER_UNAVAILABLE", 503);
            }

            JsonNode json = objectMapper.readTree(response.body());

            if (json.has("error") && "ZIP_NOT_SERVED".equals(json.get("error").asText())) {
                throw new BusinessException("SHIPPING_METHOD_UNAVAILABLE", 422);
            }

            int priceCents = json.get("price_cents").asInt();
            int etaHours = json.get("eta_hours").asInt();

            BigDecimal cost = new BigDecimal(priceCents)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);

            int deliveryDays = (int) Math.ceil((double) etaHours / 24);

            int additionalDays = switch (region) {
                case NORTH -> 3;
                case NORTHEAST -> 2;
                default -> 0;
            };

            return new ShippingQuote(cost, deliveryDays + additionalDays);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("CARRIER_UNAVAILABLE", 503);
        }
    }
}
