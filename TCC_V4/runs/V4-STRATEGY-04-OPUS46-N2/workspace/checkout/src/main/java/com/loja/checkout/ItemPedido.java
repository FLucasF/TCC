package com.loja.checkout;

import java.math.BigDecimal;

public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {
}
