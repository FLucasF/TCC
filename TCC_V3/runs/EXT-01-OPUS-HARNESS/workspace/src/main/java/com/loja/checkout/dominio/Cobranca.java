package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Como a forma de pagamento fecha a conta: o que o cliente paga e em quantas parcelas. */
public record Cobranca(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {
}
