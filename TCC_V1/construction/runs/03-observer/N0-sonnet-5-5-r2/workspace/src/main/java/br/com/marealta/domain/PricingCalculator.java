package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class PricingCalculator {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    private PricingCalculator() {
    }

    /** Alta temporada: 15/12 até o fim de fevereiro, e julho inteiro. */
    public static boolean isHighSeason(LocalDate night) {
        int month = night.getMonthValue();
        return month == 1 || month == 2 || month == 7
                || (month == 12 && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = Money.round(room.nightlyRate());
        return isHighSeason(night) ? Money.round(rate.multiply(HIGH_SEASON_FACTOR)) : rate;
    }

    public static Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = Money.round(subtotal.add(nightPrice(room, night)));
            nights++;
        }
        BigDecimal discount = plan == RatePlan.NON_REFUNDABLE
                ? Money.round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT))
                : Money.ZERO;
        BigDecimal total = Money.round(subtotal.subtract(discount));
        return new Quote(nights, subtotal, discount, total);
    }
}
