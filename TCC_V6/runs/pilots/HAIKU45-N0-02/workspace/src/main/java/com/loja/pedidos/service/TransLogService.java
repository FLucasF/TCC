package com.loja.pedidos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.pedidos.exception.PedidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TransLogService {
    @Value("${translog.url}")
    private String translogUrl;

    @Value("${translog.timeout.seconds:2}")
    private long timeoutSeconds;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TransLogService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public FreteResult consultarFrete(String cep, BigDecimal pesoKg, String service) {
        try {
            long weightGrams = pesoKg.multiply(new BigDecimal("1000")).longValue();
            String url = String.format("%s/v2/quote?zip=%s&weight_g=%d&service=%s",
                translogUrl, cep, weightGrams, service);

            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response.has("error")) {
                if ("ZIP_NOT_SERVED".equals(response.get("error").asText())) {
                    return new FreteResult(false, null, null);
                }
            }

            long priceCents = response.get("price_cents").asLong();
            long etaHours = response.get("eta_hours").asLong();

            BigDecimal price = new BigDecimal(priceCents).divide(new BigDecimal("100"), 2, RoundingMode.HALF_EVEN);
            int dias = (int) Math.ceil((double) etaHours / 24);

            return new FreteResult(true, price, dias);
        } catch (RestClientException e) {
            throw new PedidoException("TRANSPORTADORA_INDISPONIVEL",
                "TransLog unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            throw new PedidoException("TRANSPORTADORA_INDISPONIVEL",
                "TransLog unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public static class FreteResult {
        public boolean available;
        public BigDecimal price;
        public Integer dias;

        public FreteResult(boolean available, BigDecimal price, Integer dias) {
            this.available = available;
            this.price = price;
            this.dias = dias;
        }
    }
}
