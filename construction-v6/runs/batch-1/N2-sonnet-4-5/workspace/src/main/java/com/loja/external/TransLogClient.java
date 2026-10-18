package com.loja.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class TransLogClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public TransLogClient(@Value("${translog.url}") String translogUrl) {
        this.baseUrl = translogUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);
        this.restTemplate = new RestTemplate(factory);
    }

    public TransLogQuote getQuote(String zipCode, BigDecimal weightKg, String service) {
        try {
            int weightGrams = weightKg.multiply(new BigDecimal("1000")).intValue();

            String url = baseUrl + "/v2/quote?zip=" + zipCode + "&weight_g=" + weightGrams + "&service=" + service;

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("error")) {
                String error = (String) response.get("error");
                if ("ZIP_NOT_SERVED".equals(error)) {
                    return new TransLogQuote(false, null, null);
                }
                throw new RuntimeException("TransLog error: " + error);
            }

            if (response == null) {
                return new TransLogQuote(false, null, null);
            }

            Integer priceCents = (Integer) response.get("price_cents");
            Integer etaHours = (Integer) response.get("eta_hours");

            BigDecimal price = new BigDecimal(priceCents).divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
            int days = (int) Math.ceil(etaHours / 24.0);

            return new TransLogQuote(true, price, days);
        } catch (Exception e) {
            return new TransLogQuote(false, null, null);
        }
    }

    public static class TransLogQuote {
        private final boolean available;
        private final BigDecimal price;
        private final Integer deliveryDays;

        public TransLogQuote(boolean available, BigDecimal price, Integer deliveryDays) {
            this.available = available;
            this.price = price;
            this.deliveryDays = deliveryDays;
        }

        public boolean isAvailable() { return available; }
        public BigDecimal getPrice() { return price; }
        public Integer getDeliveryDays() { return deliveryDays; }
    }
}
