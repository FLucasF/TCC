package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Resultado da forma de pagamento escolhida. */
public record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {
}
