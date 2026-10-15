package com.loja.pedidos.client;

import com.loja.pedidos.config.TransLogConfig;
import com.loja.pedidos.exception.ErroNegocio;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransLogClient {
    private final TransLogConfig config;
    private final RestTemplate restTemplate;

    public TransLogCotacao cotacao(String cep, BigDecimal pesoKg, String servico) {
        try {
            String url = config.getUrl() + "/cotacao?cep=" + cep + "&pesoKg=" + pesoKg + "&servico=" + servico;

            log.debug("Chamando TransLog: {}", url);
            var response = restTemplate.getForObject(url, TransLogCotacao.class);
            return response;
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("CEP não atendido pela TransLog");
            return null;
        } catch (ResourceAccessException e) {
            log.warn("Timeout ao chamar TransLog");
            throw ErroNegocio.transportadoraIndisponivel();
        } catch (Exception e) {
            log.error("Erro ao chamar TransLog", e);
            throw ErroNegocio.transportadoraIndisponivel();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransLogCotacao {
        private BigDecimal valor;
        private Integer prazoDias;
    }
}
