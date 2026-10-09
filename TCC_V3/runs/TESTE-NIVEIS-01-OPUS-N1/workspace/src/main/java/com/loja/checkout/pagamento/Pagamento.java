package com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** O que o cliente paga de fato: valor final, em quantas vezes e quanto por parcela. */
public record Pagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
