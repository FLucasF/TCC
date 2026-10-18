package com.loja.orderservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransLogClient {
    @Value("${translog.url:http://localhost:8081}")
    private String translogUrl;

    private final ObjectMapper objectMapper;

    public Optional<TransLogQuoteResponse> getQuote(String zipCode, Integer weightGrams, String service) {
        try {
            String url = String.format("%s/v2/quote?zip=%s&weight_g=%d&service=%s",
                    translogUrl, zipCode, weightGrams, service);

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                @SuppressWarnings("unchecked")
                Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);

                if (responseBody.containsKey("error")) {
                    if ("ZIP_NOT_SERVED".equals(responseBody.get("error"))) {
                        return Optional.empty();
                    }
                    return Optional.empty();
                }

                Integer priceCents = ((Number) responseBody.get("price_cents")).intValue();
                Integer etaHours = ((Number) responseBody.get("eta_hours")).intValue();

                return Optional.of(TransLogQuoteResponse.builder()
                        .priceCents(priceCents)
                        .etaHours(etaHours)
                        .build());
            }

            return Optional.empty();
        } catch (IOException | InterruptedException e) {
            return Optional.empty();
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @lombok.Builder
    public static class TransLogQuoteResponse {
        private Integer priceCents;
        private Integer etaHours;
    }
}
