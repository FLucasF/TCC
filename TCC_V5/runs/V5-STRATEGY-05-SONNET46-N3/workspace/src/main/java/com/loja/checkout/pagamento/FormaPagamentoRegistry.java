package com.loja.checkout.pagamento;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FormaPagamentoRegistry {

    private final Map<String, FormaPagamento> formas = Map.of(
            "PIX", new PagamentoPix(),
            "CARTAO", new PagamentoCartao(),
            "BOLETO", new PagamentoBoleto()
    );

    public FormaPagamento buscar(String codigo) {
        return formas.get(codigo);
    }
}
