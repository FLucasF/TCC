package com.loja.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class TransLogClient {

    private final String baseUrl;
    private final RestClient restClient;

    public TransLogClient(@Value("${translog.url}") String baseUrl,
                          @Value("${translog.timeout}") int timeoutMs) {
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    public QuoteResult getQuote(String zipCode, int weightGrams, String service) {
        try {
            String url = String.format("%s/v2/quote?zip=%s&weight_g=%d&service=%s",
                baseUrl, zipCode, weightGrams, service);

            QuoteResponse response = restClient.get()
                .uri(url)
                .retrieve()
                .body(QuoteResponse.class);

            if (response == null || response.error != null) {
                return null;
            }

            return new QuoteResult(response.priceCents, response.etaHours);
        } catch (Exception e) {
            throw new TransLogUnavailableException("TransLog is unavailable", e);
        }
    }

    private static class QuoteResponse {
        public Integer priceCents;
        public Integer etaHours;
        public String error;
    }

    public record QuoteResult(Integer priceCents, Integer etaHours) {}
}
