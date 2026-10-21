package pousada.domain;

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
        return new Quote(nights, subtotal, discount, subtotal.subtract(discount));
    }

    private static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return isHighSeason(night) ? Money.round(rate.multiply(HIGH_SEASON_MARKUP)) : rate;
    }

    static boolean isHighSeason(LocalDate night) {
        int month = night.getMonthValue();
        boolean summerHoliday = month == 12 && night.getDayOfMonth() >= 15 || month == 1 || month == 2;
        return summerHoliday || month == 7;
    }
}
