package com.loja.checkout;

import java.math.BigDecimal;

/** Um produto do carrinho como o site envia, ainda sem nenhuma validação. */
public record ItemPedidoCheckout(String nome,
                                 BigDecimal precoUnitario,
                                 Integer quantidade,
                                 BigDecimal pesoKg) {
}
