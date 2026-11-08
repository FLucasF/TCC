package com.pousada.rateplan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public interface RatePlan {
    void validate(int roomCapacity, int nights, LocalDate checkIn, LocalDate checkOut,
                  int guests, Map<String, Object> context) throws RatePlanException;

    PriceCalculation calculate(BigDecimal nightlyRate, int nights, LocalDate checkIn, LocalDate checkOut,
                               Map<String, Object> context);

    BigDecimal calculateRefund(BigDecimal amountPaid);

    public static class PriceCalculation {
        public final BigDecimal subtotal;
        public final BigDecimal discount;
        public final BigDecimal total;

        public PriceCalculation(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }
}
