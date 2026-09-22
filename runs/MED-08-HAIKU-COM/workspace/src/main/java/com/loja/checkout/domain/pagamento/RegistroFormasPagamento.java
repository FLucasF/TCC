package com.loja.checkout.domain.pagamento;

import java.util.HashMap;
import java.util.Map;

public class RegistroFormasPagamento {
    private static final Map<String, FormaPagamento> FORMAS = new HashMap<>();

    static {
        FORMAS.put("PIX", new FormaPagamentoPix());
        FORMAS.put("CARTAO", new FormaPagamentoCartao());
        FORMAS.put("BOLETO", new FormaPagamentoBoleto());
    }

    public static FormaPagamento obter(String codigo) {
        return FORMAS.get(codigo);
    }

    public static boolean existe(String codigo) {
        return FORMAS.containsKey(codigo);
    }
}
