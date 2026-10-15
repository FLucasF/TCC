package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** O que a forma de pagamento devolve: valor final, parcela e o ajuste aplicado. */
public record ResultadoPagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela,
                                 BigDecimal ajuste) {
}
