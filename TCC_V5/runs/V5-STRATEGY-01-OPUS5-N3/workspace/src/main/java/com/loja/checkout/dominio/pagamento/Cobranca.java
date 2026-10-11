package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/** O que a forma de pagamento devolve: o valor final e a parcela. */
public record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {
}
