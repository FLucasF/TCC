package com.loja.checkout.domain.pagamento;

import java.util.HashMap;
import java.util.Map;

public class FormaPagamentoFactory {

    private static final Map<String, FormaPagamento> formasPagamento = new HashMap<>();

    static {
        formasPagamento.put("PIX", new Pix());
        formasPagamento.put("CARTAO", new Cartao());
        formasPagamento.put("BOLETO", new Boleto());
    }

    public static FormaPagamento obter(String codigo) {
        return formasPagamento.get(codigo);
    }
}
