package br.tcc.checkout;

import java.math.BigDecimal;

record ItemPedido(BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {
}
