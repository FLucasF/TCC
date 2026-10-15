package br.tcc.checkout.api;

import java.math.BigDecimal;

/** O resumo que o site mostra antes de confirmar. */
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
