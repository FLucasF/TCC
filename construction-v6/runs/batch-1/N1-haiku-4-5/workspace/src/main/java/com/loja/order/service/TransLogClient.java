package com.loja.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TransLogClient {
  private final String translogUrl;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public TransLogClient(
    @Value("${translog.url:}") String translogUrl,
    ObjectMapper objectMapper
  ) {
    this.translogUrl = translogUrl;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(2))
      .build();
  }

  public record QuoteResponse(BigDecimal price, int etaDays) {}

  public QuoteResponse quote(String zipCode, int weightGrams, String service) throws Exception {
    if (translogUrl == null || translogUrl.isEmpty()) {
      throw new TransLogUnavailableException();
    }

    String url = String.format("%s/v2/quote?zip=%s&weight_g=%d&service=%s",
      translogUrl, zipCode, weightGrams, service);

    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .GET()
      .timeout(Duration.ofSeconds(2))
      .build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new TransLogUnavailableException();
      }

      JsonNode json = objectMapper.readTree(response.body());

      if (json.has("error")) {
        throw new ZipCodeNotServedException();
      }

      if (!json.has("price_cents") || !json.has("eta_hours")) {
        throw new TransLogUnavailableException();
      }

      long priceCents = json.get("price_cents").asLong();
      int etaHours = json.get("eta_hours").asInt();

      BigDecimal priceReais = BigDecimal.valueOf(priceCents).divide(
        new BigDecimal("100"),
        2,
        java.math.RoundingMode.HALF_EVEN
      );

      int etaDays = (int) Math.ceil(etaHours / 24.0);

      return new QuoteResponse(priceReais, etaDays);
    } catch (java.net.http.HttpTimeoutException | java.net.ConnectException e) {
      throw new TransLogUnavailableException();
    }
  }

  public static class TransLogUnavailableException extends Exception {}
  public static class ZipCodeNotServedException extends Exception {}
}
