package com.loja.checkout.domain.pagamento;

import java.util.HashMap;
import java.util.Map;

public class FormaPagamentoRegistry {
    private static final Map<String, FormaPagamento> formas = new HashMap<>();

    static {
        formas.put("PIX", new Pix());
        formas.put("BOLETO", new Boleto());
        formas.put("CARTAO", new Cartao());
    }

    public static FormaPagamento obter(String codigo) {
        return formas.get(codigo);
    }
}
