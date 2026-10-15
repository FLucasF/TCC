package com.loja.checkout.domain.pagamento;

import java.util.HashMap;
import java.util.Map;

public class FormaPagamentoFactory {

    private static final Map<String, FormaPagamento> FORMAS = new HashMap<>();

    static {
        FORMAS.put("PIX", new Pix());
        FORMAS.put("BOLETO", new Boleto());
        FORMAS.put("CARTAO", new Cartao());
    }

    public static FormaPagamento criar(String codigo) {
        return FORMAS.get(codigo);
    }

    public static boolean existe(String codigo) {
        return FORMAS.containsKey(codigo);
    }
}
