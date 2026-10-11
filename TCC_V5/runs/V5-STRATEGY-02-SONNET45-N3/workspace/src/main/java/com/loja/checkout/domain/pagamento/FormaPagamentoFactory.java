package com.loja.checkout.domain.pagamento;

import java.util.Map;

public class FormaPagamentoFactory {

    private static final Map<String, FormaPagamento> FORMAS = Map.of(
        "PIX", new Pix(),
        "CARTAO", new Cartao(),
        "BOLETO", new Boleto()
    );

    public static FormaPagamento obter(String codigo) {
        return FORMAS.get(codigo);
    }

    public static boolean existe(String codigo) {
        return FORMAS.containsKey(codigo);
    }
}
