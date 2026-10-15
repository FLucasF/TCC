package com.loja.checkout.dto;

import java.math.BigDecimal;

public record ItemPedidoDto(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
