package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Resultado do ajuste da forma de pagamento sobre o total do pedido. */
public record ResultadoPagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
