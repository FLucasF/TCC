package com.loja.checkout.model;

import java.math.BigDecimal;

public record ItemPedido(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
