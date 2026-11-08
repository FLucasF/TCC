package br.com.pousadamarealta.reservas.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class Pricing {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    private Pricing() {}

    /** Alta temporada: de 15/12 ao fim de fevereiro, e julho inteiro. A data é a da noite. */
    public static boolean isHighSeason(LocalDate night) {
        int month = night.getMonthValue();
        return month == 1 || month == 2 || month == 7 || (month == 12 && night.getDayOfMonth() >= 15);
    }

    public static Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            BigDecimal price = isHighSeason(night)
                    ? Money.round(room.nightlyRate().multiply(HIGH_SEASON_FACTOR))
                    : room.nightlyRate();
            subtotal = Money.round(subtotal.add(price));
            nights++;
        }
        BigDecimal discount = plan == RatePlan.NON_REFUNDABLE
                ? Money.round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT))
                : Money.ZERO;
        BigDecimal total = Money.round(subtotal.subtract(discount));
        return new Quote(nights, subtotal, discount, total);
    }
}
