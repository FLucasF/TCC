package br.com.mealta.domain;

import java.math.BigDecimal;

public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
}
