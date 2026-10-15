package com.loja.checkout.factory;

import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.pagamento.*;
import java.util.Map;

public class FormaPagamentoFactory {

    private static final Map<String, FormaPagamento> FORMAS = Map.of(
        "PIX", new Pix(),
        "BOLETO", new Boleto(),
        "CARTAO", new Cartao()
    );

    public static FormaPagamento obter(String codigo) {
        return FORMAS.get(codigo);
    }

    public static boolean existe(String codigo) {
        return FORMAS.containsKey(codigo);
    }
}
