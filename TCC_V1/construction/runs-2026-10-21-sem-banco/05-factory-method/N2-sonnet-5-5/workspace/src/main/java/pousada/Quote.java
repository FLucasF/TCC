package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    public static Quote of(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, night));
        }
        BigDecimal discount = plan.discountOn(subtotal);
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        return new Quote(nights, subtotal, discount, subtotal.subtract(discount));
    }

    private static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return Money.round(isHighSeason(night) ? rate.multiply(HIGH_SEASON_FACTOR) : rate);
    }

    /** 15 de dezembro até o fim de fevereiro, e julho inteiro. */
    static boolean isHighSeason(LocalDate day) {
        Month month = day.getMonth();
        return month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY
                || (month == Month.DECEMBER && day.getDayOfMonth() >= 15);
    }
}
