package com.loja.infrastructure;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TransLogClient {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final Gson gson;

    public TransLogClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        this.gson = new Gson();
    }

    public TransLogQuote getQuote(String zipCode, double weightKg, TransLogService service) throws TransLogException {
        int weightG = (int) (weightKg * 1000);
        String serviceCode = service == TransLogService.ECONOMY ? "STD" : "EXP";

        String url = String.format("%s/v2/quote?zip=%s&weight_g=%d&service=%s",
                baseUrl, zipCode, weightG, serviceCode);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new TransLogException("TransLog returned status " + response.statusCode());
            }

            JsonObject json = gson.fromJson(response.body(), JsonObject.class);

            if (json.has("error")) {
                String error = json.get("error").getAsString();
                if ("ZIP_NOT_SERVED".equals(error)) {
                    throw new ZipNotServedException("ZIP not served by TransLog");
                }
                throw new TransLogException("TransLog error: " + error);
            }

            int priceCents = json.get("price_cents").getAsInt();
            int etaHours = json.get("eta_hours").getAsInt();

            return new TransLogQuote(priceCents, etaHours);

        } catch (IOException | InterruptedException e) {
            throw new TransLogException("Failed to connect to TransLog", e);
        }
    }

    public enum TransLogService {
        ECONOMY,
        EXPRESS
    }

    public static class TransLogQuote {
        private final int priceCents;
        private final int etaHours;

        public TransLogQuote(int priceCents, int etaHours) {
            this.priceCents = priceCents;
            this.etaHours = etaHours;
        }

        public double getPriceReais() {
            return priceCents / 100.0;
        }

        public int getDeliveryDays() {
            return (int) Math.ceil(etaHours / 24.0);
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

    public static class ZipNotServedException extends TransLogException {
        public ZipNotServedException(String message) {
            super(message);
        }
    }
}
