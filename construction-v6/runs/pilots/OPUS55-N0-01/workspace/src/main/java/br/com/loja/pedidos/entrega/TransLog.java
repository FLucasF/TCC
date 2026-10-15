package br.com.loja.pedidos.entrega;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Cliente do site da TransLog. */
@Component
public class TransLog implements Transportadora {

    private static final Duration TEMPO_LIMITE = Duration.ofSeconds(2);

    private final String url;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TEMPO_LIMITE).build();

    public TransLog(@Value("${translog.url}") String url, ObjectMapper json) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.json = json;
    }

    @Override
    public Optional<CotacaoEntrega> cotar(String cep, BigDecimal pesoKg, String servico) {
        String consulta = url + "/cotacao?cep=" + codificar(cep) + "&pesoKg=" + codificar(formatarPeso(pesoKg))
                + "&servico=" + codificar(servico);
        HttpResponse<String> resposta;
        try {
            HttpRequest requisicao = HttpRequest.newBuilder(URI.create(consulta)).timeout(TEMPO_LIMITE).GET().build();
            resposta = http.sendAsync(requisicao, HttpResponse.BodyHandlers.ofString())
                    .get(TEMPO_LIMITE.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransportadoraIndisponivelException("Consulta à TransLog interrompida", e);
        } catch (Exception e) {
            throw new TransportadoraIndisponivelException("TransLog não respondeu", e);
        }
        if (resposta.statusCode() == 404) {
            return Optional.empty();
        }
        if (resposta.statusCode() != 200) {
            throw new TransportadoraIndisponivelException("TransLog respondeu " + resposta.statusCode(), null);
        }
        try {
            JsonNode corpo = json.readTree(resposta.body());
            JsonNode valor = corpo.get("valor");
            JsonNode prazo = corpo.get("prazoDias");
            if (valor == null || !valor.isNumber() || prazo == null || !prazo.isIntegralNumber()
                    || !prazo.canConvertToInt() || valor.decimalValue().signum() < 0 || prazo.intValue() < 0) {
                throw new IOException("Resposta inesperada da TransLog: " + resposta.body());
            }
            return Optional.of(new CotacaoEntrega(valor.decimalValue(), prazo.intValue()));
        } catch (Exception e) {
            throw new TransportadoraIndisponivelException("Resposta inválida da TransLog", e);
        }
    }

    private static String formatarPeso(BigDecimal pesoKg) {
        BigDecimal peso = pesoKg.stripTrailingZeros();
        return (peso.scale() < 2 ? peso.setScale(2, RoundingMode.UNNECESSARY) : peso).toPlainString();
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
