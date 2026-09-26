package br.tcc.checkout.api;

import java.util.List;

/** Corpo de {@code POST /checkout/resumo}. */
public record ResumoRequest(
		List<ItemRequest> itens,
		String modalidadeEntrega,
		String cupom,
		String formaPagamento,
		Integer parcelas) {
}
