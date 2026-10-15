package com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

/** Quanto o cliente paga no fim e quanto fica cada parcela. */
public record Pagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
