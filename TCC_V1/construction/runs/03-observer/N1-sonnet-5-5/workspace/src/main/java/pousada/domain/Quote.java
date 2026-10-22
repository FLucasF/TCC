package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    public static Quote of(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(Season.nightPrice(room.nightlyRate(), night));
        }
        subtotal = Money.round(subtotal);
        BigDecimal discount = plan.discountOn(subtotal);
        return new Quote(nightsBetween(checkIn, checkOut), subtotal, discount,
                Money.round(subtotal.subtract(discount)));
    }

    public static int nightsBetween(LocalDate checkIn, LocalDate checkOut) {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
