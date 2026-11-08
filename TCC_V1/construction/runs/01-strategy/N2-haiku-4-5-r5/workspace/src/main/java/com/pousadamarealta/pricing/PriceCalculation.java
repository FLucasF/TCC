package com.pousadamarealta.pricing;

import java.math.BigDecimal;

public record PriceCalculation(
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {}
