package br.com.mare.pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public final class PricingPolicy {
    private static final BigDecimal HIGH_SEASON_MARKUP = new BigDecimal("1.20");

    private PricingPolicy() {
    }

    public static boolean isHighSeason(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.type().nightlyRate();
        return isHighSeason(night) ? Money.percent(rate, HIGH_SEASON_MARKUP) : Money.round(rate);
    }

    public static Quote quote(Room room, RatePlan plan, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = Money.round(subtotal.add(nightPrice(room, night)));
            nights++;
        }
        BigDecimal discount = plan.discountOn(subtotal);
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }
}
