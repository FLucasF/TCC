package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class Pricing {
    private Pricing() {
    }

    /** Alta temporada: de 15 de dezembro ao fim de fevereiro, e julho inteiro. */
    public static boolean isHighSeason(LocalDate night) {
        int month = night.getMonthValue();
        return month == 1 || month == 2 || month == 7 || (month == 12 && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        return isHighSeason(night) ? Money.times(room.nightlyRate(), "1.20") : room.nightlyRate();
    }

    public static Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = Money.round(subtotal.add(nightPrice(room, night)));
        }
        BigDecimal discount = plan.discountOn(subtotal);
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }

    public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    }
}
