package com.loja.orderservice.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Component
public class TransLogClient {
    @Value("${translog.url:http://localhost:8080}")
    private String translogUrl;

    private final RestTemplate restTemplate;

    public TransLogClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public TransLogQuote getQuote(String zipCode, Integer weightG, String service) throws TransLogException {
        try {
            String url = String.format("%s/v2/quote?zip=%s&weight_g=%d&service=%s",
                translogUrl, zipCode, weightG, service);
            TransLogResponse response = restTemplate.getForObject(url, TransLogResponse.class);

            if (response == null) {
                throw new TransLogException("No response from TransLog");
            }

            if (response.error != null) {
                if ("ZIP_NOT_SERVED".equals(response.error)) {
                    throw new TransLogException("ZIP_NOT_SERVED");
                }
                throw new TransLogException("TransLog error: " + response.error);
            }

            return new TransLogQuote(response.priceCents, response.etaHours);
        } catch (RestClientException e) {
            throw new TransLogException("TransLog unavailable", e);
        }
    }

    public static class TransLogResponse {
        @JsonProperty("price_cents")
        public Integer priceCents;

        @JsonProperty("eta_hours")
        public Integer etaHours;

        public String error;
    }

    public static class TransLogQuote {
        public final Integer priceCents;
        public final Integer etaHours;

        public TransLogQuote(Integer priceCents, Integer etaHours) {
            this.priceCents = priceCents;
            this.etaHours = etaHours;
        }

        public BigDecimal getPrice() {
            return new BigDecimal(priceCents).divide(new BigDecimal("100"));
        }
    }

    public static class TransLogException extends Exception {
        public TransLogException(String message) {
            super(message);
        }

        public TransLogException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
