package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Como o pedido sera cobrado depois do ajuste da forma de pagamento. */
public record Cobranca(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
