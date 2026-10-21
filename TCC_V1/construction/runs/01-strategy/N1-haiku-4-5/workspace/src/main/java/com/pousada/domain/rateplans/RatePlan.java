package com.pousada.domain.rateplans;

import com.pousada.domain.Room;
import java.time.LocalDate;

public abstract class RatePlan {
    public abstract String getPlanName();

    public abstract void validate(Room room, LocalDate checkIn, LocalDate checkOut,
                                  int guests, String companyCode) throws IllegalArgumentException;

    public abstract PriceCalculation calculatePrices(Room room, LocalDate checkIn, LocalDate checkOut);

    public abstract double calculateRefund(double amountPaid);

    public static class PriceCalculation {
        public final double subtotal;
        public final double discount;
        public final double total;

        public PriceCalculation(double subtotal, double discount, double total) {
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }
    }
}
