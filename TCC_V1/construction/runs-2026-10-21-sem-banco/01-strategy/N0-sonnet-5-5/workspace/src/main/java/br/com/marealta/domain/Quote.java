package br.com.marealta.domain;

import java.math.BigDecimal;

/** Resultado do cálculo de preço de uma reserva. */
public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    public static Quote of(int nights, BigDecimal subtotal, BigDecimal discount) {
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }
}
