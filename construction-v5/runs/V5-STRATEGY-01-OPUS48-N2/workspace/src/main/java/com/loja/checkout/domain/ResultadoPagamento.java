package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * O que a forma de pagamento produz a partir do total do pedido: o ajuste
 * (valor final menos total), o valor final, a parcela e o número de parcelas.
 */
public record ResultadoPagamento(
        BigDecimal ajuste,
        BigDecimal totalFinal,
        BigDecimal valorParcela,
        int parcelas) {
}
