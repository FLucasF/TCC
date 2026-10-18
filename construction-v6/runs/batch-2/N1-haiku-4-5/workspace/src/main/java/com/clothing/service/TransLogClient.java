package com.clothing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TransLogClient {
    private static final Logger logger = LoggerFactory.getLogger(TransLogClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public record ShippingQuote(BigDecimal price, int deliveryDays) {}

    public static ShippingQuote quote(String zipCode, double weightKg, String service) throws CarrierUnavailableException {
        String translogUrl = System.getenv("TRANSLOG_URL");
        if (translogUrl == null) {
            throw new CarrierUnavailableException();
        }

        int weightGrams = (int) Math.round(weightKg * 1000);
        String url = String.format("%s/v2/quote?zip=%s&weight_g=%d&service=%s", translogUrl, zipCode, weightGrams, service);

        try {
            var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.warn("TransLog returned status {}", response.statusCode());
                throw new CarrierUnavailableException();
            }

            JsonNode node = mapper.readTree(response.body());

            if (node.has("error")) {
                return null;
            }

            if (!node.has("price_cents") || !node.has("eta_hours")) {
                throw new CarrierUnavailableException();
            }

            long priceCents = node.get("price_cents").asLong();
            BigDecimal priceReais = Money.divide(new BigDecimal(priceCents), new BigDecimal("100"));

            int hoursEta = node.get("eta_hours").asInt();
            int deliveryDays = (int) Math.ceil((double) hoursEta / 24);

            return new ShippingQuote(priceReais, deliveryDays);
        } catch (IOException | InterruptedException e) {
            logger.error("Failed to contact TransLog", e);
            throw new CarrierUnavailableException();
        }
    }

    public static class CarrierUnavailableException extends Exception {
        public CarrierUnavailableException() {
            super("Carrier unavailable");
        }
    }
}
