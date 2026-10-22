package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    public static Quote of(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(Season.priceOf(room, night));
        }
        BigDecimal discount = plan.discountOn(subtotal);
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }
}
