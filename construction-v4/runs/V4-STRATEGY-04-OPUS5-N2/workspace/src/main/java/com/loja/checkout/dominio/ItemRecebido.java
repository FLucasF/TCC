package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Um produto como o site envia, ainda sem validar. */
public record ItemRecebido(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {

    public Item validado() {
        ErroPedido.PEDIDO_INVALIDO.exigir(positivo(precoUnitario)
                && quantidade != null && quantidade > 0
                && positivo(pesoKg));
        return new Item(nome, precoUnitario, quantidade, pesoKg);
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }
}
