package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Catalogo;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** As formas de pagamento aceitas hoje. */
public final class Pagamentos {

    public static final Catalogo<FormaPagamento> CATALOGO = new Catalogo<>(
            List.of(new Pix(), new Cartao(), new Boleto()).stream()
                    .collect(Collectors.toMap(FormaPagamento::codigo, Function.identity())),
            "FORMA_PAGAMENTO_INVALIDA");

    private Pagamentos() {
    }
}
