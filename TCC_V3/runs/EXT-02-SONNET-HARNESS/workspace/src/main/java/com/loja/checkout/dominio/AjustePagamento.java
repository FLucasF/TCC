package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record AjustePagamento(BigDecimal ajuste, BigDecimal totalFinal, BigDecimal valorParcela) {
}
