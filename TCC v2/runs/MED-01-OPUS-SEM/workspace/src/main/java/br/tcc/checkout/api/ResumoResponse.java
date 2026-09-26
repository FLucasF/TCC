package br.tcc.checkout.api;

import java.math.BigDecimal;

/** Resumo mostrado ao cliente antes de confirmar o pedido. */
public record ResumoResponse(
		BigDecimal subtotalProdutos,
		BigDecimal descontoCupom,
		BigDecimal frete,
		int prazoEntregaDias,
		BigDecimal ajustePagamento,
		BigDecimal totalFinal,
		int parcelas,
		BigDecimal valorParcela) {
}
