package com.pousadamarealta.tariff;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TarifPlan {
    BigDecimal calculateSubtotal(BigDecimal nightlyRate, int nights, LocalDate checkIn);

    BigDecimal calculateDiscount(BigDecimal subtotal);

    BigDecimal calculateRefund(BigDecimal amountPaid);
}
