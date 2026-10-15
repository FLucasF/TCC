package br.tcc.checkout.api.dto;

import java.math.BigDecimal;

public record ItemPedidoRequest(
		String nome,
		BigDecimal precoUnitario,
		Integer quantidade,
		BigDecimal pesoKg) {
}
