package br.tcc.checkout.pagamento;

import java.math.BigDecimal;

public record ResultadoPagamento(
		BigDecimal ajuste,
		BigDecimal totalFinal,
		int parcelas,
		BigDecimal valorParcela) {
}
