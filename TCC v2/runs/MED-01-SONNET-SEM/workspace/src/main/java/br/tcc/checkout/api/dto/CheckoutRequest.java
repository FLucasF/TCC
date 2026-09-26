package br.tcc.checkout.api.dto;

import java.util.List;

public record CheckoutRequest(
		List<ItemPedidoRequest> itens,
		String modalidadeEntrega,
		String cupom,
		String formaPagamento,
		Integer parcelas) {
}
