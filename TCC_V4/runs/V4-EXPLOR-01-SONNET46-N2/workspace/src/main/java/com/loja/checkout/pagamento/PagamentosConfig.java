package com.loja.checkout.pagamento;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class PagamentosConfig {

    @Bean
    public Map<String, FormaPagamento> formasPagamento() {
        return Map.of(
                "PIX", new PagamentoPix(),
                "CARTAO", new PagamentoCartao(),
                "BOLETO", new PagamentoBoleto()
        );
    }
}
