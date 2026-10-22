package br.com.marealta;

import java.math.BigDecimal;

public record Price(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}
