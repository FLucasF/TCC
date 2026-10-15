package com.loja.checkout.pedido;

import java.math.BigDecimal;

public record ItemPedido(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
