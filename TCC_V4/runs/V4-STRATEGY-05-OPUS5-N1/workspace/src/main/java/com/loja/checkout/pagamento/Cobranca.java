package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** O que a forma de pagamento escolhida faz com o total do pedido. */
public record Cobranca(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
