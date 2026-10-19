package com.pousadamarealta.service;

import java.math.BigDecimal;

public record PricingResult(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
}
