package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    public static Quote of(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = Money.round(subtotal.add(Season.priceOf(room.nightlyRate(), night)));
            nights++;
        }
        BigDecimal discount = plan.discountOn(subtotal);
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }
}
