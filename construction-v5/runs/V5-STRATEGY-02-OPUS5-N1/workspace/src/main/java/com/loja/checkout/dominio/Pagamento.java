package com.loja.checkout.dominio;

import java.math.BigDecimal;

/** Como a forma de pagamento fecha a compra. */
public record Pagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
