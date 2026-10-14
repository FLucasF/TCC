package com.loja.pedidos.entrega;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Consulta de frete na TransLog. Trocar de transportadora = nova implementação de {@link ModalidadeEntrega}. */
@Component
public class TransLogClient {

    private static final Duration LIMITE = Duration.ofSeconds(2);

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(LIMITE).build();
    private final JsonMapper json = JsonMapper.builder().build();

    public TransLogClient(@Value("${translog.url}") String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public RespostaTransLog cotar(String cep, int pesoGramas, String servico) {
        CompletableFuture<HttpResponse<String>> futuro = null;
        try {
            URI uri = URI.create(baseUrl + "/v2/quote?zip=" + cep + "&weight_g=" + pesoGramas + "&service=" + servico);
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(LIMITE).GET().build();
            futuro = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            HttpResponse<String> resposta = futuro.get(LIMITE.toMillis(), TimeUnit.MILLISECONDS);
            if (resposta.statusCode() != 200) {
                return new RespostaTransLog.ForaDoAr();
            }
            return interpretar(json.readTree(resposta.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new RespostaTransLog.ForaDoAr();
        } catch (Exception e) {
            return new RespostaTransLog.ForaDoAr();
        } finally {
            if (futuro != null) {
                futuro.cancel(true);
            }
        }
    }

    private RespostaTransLog interpretar(JsonNode no) {
        if (no == null || !no.isObject()) {
            return new RespostaTransLog.ForaDoAr();
        }
        JsonNode erro = no.get("error");
        if (erro != null) {
            return erro.isString() && "ZIP_NOT_SERVED".equals(erro.stringValue())
                    ? new RespostaTransLog.CepNaoAtendido()
                    : new RespostaTransLog.ForaDoAr();
        }
        JsonNode preco = no.get("price_cents");
        JsonNode horas = no.get("eta_hours");
        if (preco == null || horas == null || !preco.isIntegralNumber() || !horas.isIntegralNumber()
                || !preco.canConvertToInt() || !horas.canConvertToInt()
                || preco.intValue() < 0 || horas.intValue() < 0) {
            return new RespostaTransLog.ForaDoAr();
        }
        return new RespostaTransLog.Preco(preco.intValue(), horas.intValue());
    }
}
