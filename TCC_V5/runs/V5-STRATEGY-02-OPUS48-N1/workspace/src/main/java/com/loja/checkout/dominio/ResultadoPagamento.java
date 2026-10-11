package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** O que o pagamento produz: o valor final do pedido e o valor de cada parcela. */
public record ResultadoPagamento(BigDecimal valorFinal, BigDecimal valorParcela) {
}
