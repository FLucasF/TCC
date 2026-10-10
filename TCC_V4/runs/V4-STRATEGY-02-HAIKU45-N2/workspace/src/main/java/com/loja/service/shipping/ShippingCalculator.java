package com.loja.service.shipping;

import java.math.BigDecimal;

public interface ShippingCalculator {
    BigDecimal calculate(BigDecimal totalWeightKg);
    Integer getPrazo();
    boolean isAvailable(BigDecimal totalWeightKg);
}
