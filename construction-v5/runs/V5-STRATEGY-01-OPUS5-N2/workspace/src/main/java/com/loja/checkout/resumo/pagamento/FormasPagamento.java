package com.loja.checkout.resumo.pagamento;

import java.util.Map;
import java.util.Optional;

/** As formas de pagamento aceitas, pelo código que o site envia. */
public final class FormasPagamento {

    private static final Map<String, FormaPagamento> POR_CODIGO = Map.of(
            "PIX", new Pix(),
            "CARTAO", new Cartao(),
            "BOLETO", new Boleto());

    private FormasPagamento() {
    }

    public static Optional<FormaPagamento> porCodigo(String codigo) {
        return Optional.ofNullable(codigo).map(POR_CODIGO::get);
    }
}
