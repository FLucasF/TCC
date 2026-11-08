package com.pousadamarealta.rateplans;

import com.pousadamarealta.models.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlan {
    String getName();

    void validateAppliesTo(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) throws InvalidRatePlanException;

    PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut);

    BigDecimal getRefundAmount(BigDecimal amountPaid);

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

    public static class InvalidRatePlanException extends Exception {
        public InvalidRatePlanException(String message) {
            super(message);
        }
    }
}
