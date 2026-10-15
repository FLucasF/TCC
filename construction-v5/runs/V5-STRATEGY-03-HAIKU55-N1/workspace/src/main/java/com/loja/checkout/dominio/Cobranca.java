package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {
}
