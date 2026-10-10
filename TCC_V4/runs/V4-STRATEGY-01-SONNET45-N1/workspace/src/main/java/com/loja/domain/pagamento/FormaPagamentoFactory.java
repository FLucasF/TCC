package com.loja.domain.pagamento;

import java.util.Map;

public class FormaPagamentoFactory {
    private static final Map<String, FormaPagamento> FORMAS = Map.of(
        "PIX", new Pix(),
        "BOLETO", new Boleto(),
        "CARTAO", new Cartao()
    );

    public static FormaPagamento criar(String nome) {
        return FORMAS.get(nome);
    }
}
