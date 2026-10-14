package br.com.loja.pedidos.checkout.entrega.transportadora;

import br.com.loja.pedidos.comum.Json;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Cotação de frete na TransLog: {@code GET {TRANSLOG_URL}/v2/quote?zip=…&weight_g=…&service=STD|EXP}. */
@Component
public class TransLog implements Transportadora {

    private static final Logger log = LoggerFactory.getLogger(TransLog.class);

    private final String url;
    private final Duration tempoLimite;
    private final ObjectMapper json;
    private final HttpClient http;

    public TransLog(@Value("${translog.url:}") String url,
                    @Value("${translog.tempo-limite:2s}") Duration tempoLimite,
                    ObjectMapper json) {
        this.url = url.strip().replaceAll("/+$", "");
        this.tempoLimite = tempoLimite;
        this.json = json;
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(tempoLimite)
                .build();
    }

    @Override
    public CotacaoTransportadora cotar(String cep, long pesoGramas, ServicoTransporte servico) {
        if (url.isEmpty()) {
            throw new TransportadoraForaDoArException("TRANSLOG_URL não configurada");
        }
        Map<?, ?> resposta = consultar(cep, pesoGramas, servico);
        Object erro = resposta.get("error");
        if (erro != null) {
            if ("ZIP_NOT_SERVED".equals(erro)) {
                throw new CepNaoAtendidoException(cep);
            }
            throw new TransportadoraForaDoArException("Erro da TransLog: " + erro);
        }
        Long precoCentavos = Json.inteiro(resposta.get("price_cents"));
        Long prazoHoras = Json.inteiro(resposta.get("eta_hours"));
        if (precoCentavos == null || precoCentavos < 0 || prazoHoras == null || prazoHoras < 0) {
            throw new TransportadoraForaDoArException("Resposta inesperada da TransLog: " + resposta);
        }
        return new CotacaoTransportadora(BigDecimal.valueOf(precoCentavos, 2), Math.toIntExact(Math.ceilDiv(prazoHoras, 24)));
    }

    private Map<?, ?> consultar(String cep, long pesoGramas, ServicoTransporte servico) {
        URI uri = URI.create(url + "/v2/quote?zip=" + URLEncoder.encode(cep, StandardCharsets.UTF_8)
                + "&weight_g=" + pesoGramas
                + "&service=" + codigo(servico));
        HttpRequest requisicao = HttpRequest.newBuilder(uri).timeout(tempoLimite).GET().build();
        CompletableFuture<HttpResponse<String>> futuro = http.sendAsync(requisicao, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> resposta;
        try {
            resposta = futuro.get(tempoLimite.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            futuro.cancel(true);
            throw new TransportadoraForaDoArException("TransLog não respondeu a tempo", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransportadoraForaDoArException("Consulta à TransLog interrompida", e);
        } catch (ExecutionException e) {
            log.warn("Falha ao consultar a TransLog: {}", e.getCause().toString());
            throw new TransportadoraForaDoArException("Falha ao consultar a TransLog", e.getCause());
        }
        if (resposta.statusCode() != 200) {
            throw new TransportadoraForaDoArException("TransLog respondeu " + resposta.statusCode());
        }
        try {
            if (json.readValue(resposta.body(), Object.class) instanceof Map<?, ?> corpo) {
                return corpo;
            }
        } catch (RuntimeException e) {
            throw new TransportadoraForaDoArException("Resposta ilegível da TransLog", e);
        }
        throw new TransportadoraForaDoArException("Resposta inesperada da TransLog");
    }

    private static String codigo(ServicoTransporte servico) {
        return switch (servico) {
            case ECONOMICO -> "STD";
            case EXPRESSO -> "EXP";
        };
    }
}
