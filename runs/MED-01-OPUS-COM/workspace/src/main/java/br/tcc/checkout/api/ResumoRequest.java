package br.tcc.checkout.api;

import java.math.BigDecimal;
import java.util.List;

/** O pedido como o site manda. */
public record ResumoRequest(
		List<ItemRequest> itens,
		String modalidadeEntrega,
		String cupom,
		String formaPagamento,
		Integer parcelas) {

	public record ItemRequest(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
	}
}
