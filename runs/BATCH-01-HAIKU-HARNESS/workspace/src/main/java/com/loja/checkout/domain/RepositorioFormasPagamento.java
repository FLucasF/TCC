package com.loja.checkout.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RepositorioFormasPagamento {
    private static final Map<String, FormaPagamento> formas = new HashMap<>();

    static {
        formas.put("PIX", new FormaPagamentoPix());
        formas.put("CARTAO", new FormaPagamentoCartao());
        formas.put("BOLETO", new FormaPagamentoBoleto());
    }

    public static Optional<FormaPagamento> obter(String codigo) {
        return Optional.ofNullable(formas.get(codigo));
    }

    public static boolean existe(String codigo) {
        return formas.containsKey(codigo);
    }
}
