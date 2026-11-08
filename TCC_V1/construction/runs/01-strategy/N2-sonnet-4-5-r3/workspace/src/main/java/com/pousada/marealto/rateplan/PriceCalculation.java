package com.pousada.marealto.rateplan;

import java.math.BigDecimal;

public record PriceCalculation(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
}
