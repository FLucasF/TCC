package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/** Como a forma de pagamento fecha a conta: valor final e valor de cada parcela. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
