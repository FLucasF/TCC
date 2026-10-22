package com.pousada.plan;

import java.math.BigDecimal;

public record PriceQuote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
}
