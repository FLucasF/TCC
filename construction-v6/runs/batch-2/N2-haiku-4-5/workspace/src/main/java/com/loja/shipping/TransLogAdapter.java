package com.loja.shipping;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TransLogAdapter implements ShippingCarrier {
    private final String baseUrl;
    private static final int TIMEOUT_MS = 2000;
    private final ObjectMapper mapper = new ObjectMapper();

    public TransLogAdapter(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public ShippingQuote getQuote(String zipCode, Double weightGrams) {
        return getQuoteWithService(zipCode, weightGrams, "STD");
    }

    public ShippingQuote getQuoteWithService(String zipCode, Double weightGrams, String service) {
        try {
            String url = String.format("%s/v2/quote?zip=%s&weight_g=%d&service=%s",
                baseUrl, zipCode, weightGrams.intValue(), service);

            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() == 200) {
                String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return mapper.readValue(response, ShippingQuote.class);
            }

            ShippingQuote quote = new ShippingQuote();
            quote.error = "CARRIER_UNAVAILABLE";
            return quote;

        } catch (Exception e) {
            ShippingQuote quote = new ShippingQuote();
            quote.error = "CARRIER_UNAVAILABLE";
            return quote;
        }
    }
}
