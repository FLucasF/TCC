package loja.entrega;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import loja.dominio.Dinheiro;
import loja.dominio.NegocioException;
import loja.entrega.ModalidadeEntrega.Cotacao;

@Component
public class TransLogClient implements Transportadora {

    private static final Duration LIMITE = Duration.ofSeconds(2);

    private final String baseUrl;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(LIMITE).build();

    public TransLogClient(@Value("${translog.url:}") String baseUrl, ObjectMapper mapper) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        this.mapper = mapper;
    }

    record Resposta(BigDecimal valor, Integer prazoDias) {
    }

    @Override
    public Cotacao cotar(String cep, BigDecimal pesoKg, String servico) {
        if (baseUrl.isEmpty()) {
            throw foraDoAr();
        }
        CompletableFuture<HttpResponse<String>> futuro = null;
        try {
            URI uri = URI.create(baseUrl + "/cotacao?cep=" + enc(cep) + "&pesoKg=" + enc(pesoKg.toPlainString())
                    + "&servico=" + enc(servico));
            HttpRequest req = HttpRequest.newBuilder(uri).timeout(LIMITE).GET().build();
            futuro = http.sendAsync(req, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> resp = futuro.get(LIMITE.toMillis(), TimeUnit.MILLISECONDS);
            if (resp.statusCode() == 404) {
                throw NegocioException.invalido("MODALIDADE_INDISPONIVEL");
            }
            if (resp.statusCode() != 200) {
                throw foraDoAr();
            }
            Resposta r = mapper.readValue(resp.body(), Resposta.class);
            if (r == null || r.valor() == null || r.prazoDias() == null
                    || r.valor().signum() < 0 || r.prazoDias() < 0) {
                throw foraDoAr();
            }
            return new Cotacao(Dinheiro.arredondar(r.valor()), r.prazoDias());
        } catch (NegocioException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw foraDoAr();
        } catch (Exception e) {
            if (futuro != null) {
                futuro.cancel(true);
            }
            throw foraDoAr();
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static NegocioException foraDoAr() {
        return new NegocioException("TRANSPORTADORA_INDISPONIVEL", 503);
    }
}
