package com.loja.checkout.domain;

import java.math.BigDecimal;

/**
 * Resultado do calculo da forma de pagamento: em quantas parcelas, o valor de
 * cada parcela e o valor final (ja arredondados para centavos).
 */
public record ResultadoPagamento(int parcelas, BigDecimal valorParcela, BigDecimal totalFinal) {
}
