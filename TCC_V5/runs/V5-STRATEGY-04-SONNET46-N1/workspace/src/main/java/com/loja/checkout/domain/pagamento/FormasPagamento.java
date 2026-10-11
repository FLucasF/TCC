package com.loja.checkout.domain.pagamento;

import java.util.Map;

public class FormasPagamento {
    private static final Map<String, FormaPagamento> REGISTRO = Map.of(
        "PIX", new Pix(),
        "CARTAO", new Cartao(),
        "BOLETO", new Boleto()
    );

    public static FormaPagamento buscar(String forma) {
        return REGISTRO.get(forma);
    }
}
