package br.com.marealta.pousada.domain;

import java.math.BigDecimal;

public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
}
