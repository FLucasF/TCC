package com.loja.orderservice.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

@Service
public class TransLogService {

    private final String translogUrl;
    private final RestClient restClient;

    public TransLogService(
            @Value("${translog.url}") String translogUrl,
            @Value("${translog.timeout.seconds}") int timeoutSeconds) {
        this.translogUrl = translogUrl;
        this.restClient = RestClient.builder()
                .baseUrl(translogUrl)
                .build();
    }

    public QuoteResult getQuote(String zipCode, BigDecimal weightKg, String service) {
        try {
            int weightGrams = weightKg.multiply(new BigDecimal("1000")).intValue();

            String url = String.format("/v2/quote?zip=%s&weight_g=%d&service=%s",
                    zipCode, weightGrams, service);

            TransLogResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(TransLogResponse.class);

            if (response == null) {
                return QuoteResult.unavailable();
            }

            if (response.error != null) {
                if ("ZIP_NOT_SERVED".equals(response.error)) {
                    return QuoteResult.notServed();
                }
                return QuoteResult.unavailable();
            }

            if (response.priceCents == null || response.etaHours == null) {
                return QuoteResult.unavailable();
            }

            BigDecimal price = new BigDecimal(response.priceCents)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);

            int days = (int) Math.ceil(response.etaHours / 24.0);

            return QuoteResult.success(price, days);

        } catch (Exception e) {
            return QuoteResult.unavailable();
        }
    }

    public static class QuoteResult {
        private final boolean available;
        private final boolean served;
        private final BigDecimal price;
        private final int days;

        private QuoteResult(boolean available, boolean served, BigDecimal price, int days) {
            this.available = available;
            this.served = served;
            this.price = price;
            this.days = days;
        }

        public static QuoteResult success(BigDecimal price, int days) {
            return new QuoteResult(true, true, price, days);
        }

        public static QuoteResult notServed() {
            return new QuoteResult(true, false, null, 0);
        }

        public static QuoteResult unavailable() {
            return new QuoteResult(false, false, null, 0);
        }

        public boolean isAvailable() {
            return available;
        }

        public boolean isServed() {
            return served;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public int getDays() {
            return days;
        }
    }

    private static class TransLogResponse {
        @JsonProperty("price_cents")
        private Integer priceCents;

        @JsonProperty("eta_hours")
        private Integer etaHours;

        private String error;

        public Integer getPriceCents() {
            return priceCents;
        }

        public void setPriceCents(Integer priceCents) {
            this.priceCents = priceCents;
        }

        public Integer getEtaHours() {
            return etaHours;
        }

        public void setEtaHours(Integer etaHours) {
            this.etaHours = etaHours;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }
}
