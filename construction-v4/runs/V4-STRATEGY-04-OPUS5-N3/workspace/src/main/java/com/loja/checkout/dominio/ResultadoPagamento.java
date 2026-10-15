package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** O que a forma de pagamento devolve: quanto o cliente paga no fim e quanto dá cada parcela. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
