package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Resultado do pagamento: o que o cliente paga no fim e quanto da cada parcela. */
public record Pagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
