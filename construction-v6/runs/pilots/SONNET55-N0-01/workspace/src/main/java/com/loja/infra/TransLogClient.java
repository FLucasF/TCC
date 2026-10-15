package com.loja.infra;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.loja.dominio.ErroNegocio;
import com.loja.dominio.Transportadora;

@Component
public class TransLogClient implements Transportadora {

    private static final Logger log = LoggerFactory.getLogger(TransLogClient.class);
    private static final Duration LIMITE = Duration.ofSeconds(2);

    private final String urlBase;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(LIMITE).build();
    private final JsonMapper json = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();

    public TransLogClient(@Value("${app.translog.url:}") String urlBase) {
        this.urlBase = urlBase == null ? "" : urlBase.strip().replaceAll("/+$", "");
    }

    @Override
    public Optional<Cotacao> cotar(String cep, BigDecimal pesoKg, String servico) {
        if (urlBase.isEmpty()) {
            throw foraDoAr("TRANSLOG_URL não definida", null);
        }
        URI uri = URI.create(urlBase + "/cotacao?cep=" + codificar(cep) + "&pesoKg=" + pesoKg.toPlainString()
                + "&servico=" + codificar(servico));
        HttpRequest requisicao = HttpRequest.newBuilder(uri).timeout(LIMITE).GET().build();
        CompletableFuture<HttpResponse<String>> chamada = http.sendAsync(requisicao,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        HttpResponse<String> resposta;
        try {
            resposta = chamada.get(LIMITE.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            chamada.cancel(true);
            Thread.currentThread().interrupt();
            throw foraDoAr("interrompido", e);
        } catch (Exception e) {
            chamada.cancel(true);
            throw foraDoAr("sem resposta", e);
        }
        if (resposta.statusCode() == 404) {
            return Optional.empty();
        }
        if (resposta.statusCode() != 200) {
            throw foraDoAr("status " + resposta.statusCode(), null);
        }
        try {
            JsonNode corpo = json.readTree(resposta.body());
            JsonNode valor = corpo.get("valor");
            JsonNode prazo = corpo.get("prazoDias");
            if (valor == null || !valor.isNumber() || prazo == null || !prazo.isIntegralNumber()) {
                throw foraDoAr("resposta inesperada", null);
            }
            return Optional.of(new Cotacao(valor.decimalValue(), prazo.intValue()));
        } catch (ErroNegocio e) {
            throw e;
        } catch (RuntimeException e) {
            throw foraDoAr("resposta ilegível", e);
        }
    }

    private static String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8);
    }

    private static ErroNegocio foraDoAr(String motivo, Exception causa) {
        log.warn("TransLog indisponível: {}", motivo, causa);
        return new ErroNegocio("TRANSPORTADORA_INDISPONIVEL", 503);
    }
}
