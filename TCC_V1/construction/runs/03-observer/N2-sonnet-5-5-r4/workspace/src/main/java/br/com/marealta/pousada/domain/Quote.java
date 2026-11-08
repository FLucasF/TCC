package br.com.marealta.pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    private static final BigDecimal HIGH_SEASON_MARKUP = new BigDecimal("1.20");

    public static Quote of(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
            nights++;
        }
        BigDecimal discount = plan.discountOn(subtotal);
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }

    private static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return Money.round(isHighSeason(night) ? rate.multiply(HIGH_SEASON_MARKUP) : rate);
    }

    /** De 15 de dezembro ao fim de fevereiro, e julho inteiro. */
    public static boolean isHighSeason(LocalDate day) {
        return switch (day.getMonth()) {
            case JANUARY, FEBRUARY, JULY -> true;
            case DECEMBER -> day.getDayOfMonth() >= 15;
            default -> false;
        };
    }
}
