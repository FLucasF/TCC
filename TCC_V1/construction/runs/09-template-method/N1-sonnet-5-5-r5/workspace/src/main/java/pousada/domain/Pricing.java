package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public final class Pricing {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Pricing() {
    }

    public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    }

    public static Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(priceOfNight(room.nightlyRate(), night));
            nights++;
        }
        BigDecimal discount = plan.discountOn(subtotal);
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }

    private static BigDecimal priceOfNight(BigDecimal nightlyRate, LocalDate night) {
        return isHighSeason(night) ? Money.round(nightlyRate.multiply(HIGH_SEASON_FACTOR)) : Money.round(nightlyRate);
    }

    /** De 15 de dezembro ao fim de fevereiro, e julho inteiro. */
    static boolean isHighSeason(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JULY
                || month == Month.JANUARY
                || month == Month.FEBRUARY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }
}
