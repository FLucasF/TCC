package com.loja.checkout.domain.pagamento;

import java.math.BigDecimal;

/** Como a forma de pagamento escolhida fecha a conta. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
