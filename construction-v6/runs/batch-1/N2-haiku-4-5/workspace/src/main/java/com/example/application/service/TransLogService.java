package com.example.application.service;

import com.example.domain.enums.ShippingMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class TransLogService {

    @Value("${translog.url:http://localhost:9999}")
    private String translogUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(2))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderSummaryCalculator.ShippingQuote getQuote(String zipCode, BigDecimal weightKg, ShippingMethod method) {
        try {
            String service = method == ShippingMethod.ECONOMY ? "STD" : "EXP";
            long weightGrams = Math.round(weightKg.doubleValue() * 1000);

            String url = String.format("%s/v2/quote?zip=%s&weight_g=%d&service=%s",
                    translogUrl, zipCode, weightGrams, service);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);

            if (responseBody.containsKey("error")) {
                return null;
            }

            int priceCents = ((Number) responseBody.get("price_cents")).intValue();
            int etaHours = ((Number) responseBody.get("eta_hours")).intValue();

            BigDecimal price = new BigDecimal(priceCents).divide(new BigDecimal(100));
            int days = (int) Math.ceil((double) etaHours / 24);

            return new OrderSummaryCalculator.ShippingQuote(price, days);
        } catch (IOException | InterruptedException e) {
            return null;
        }
    }
}
