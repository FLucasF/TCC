package br.com.marealta;

import br.com.marealta.plan.RatePlan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

public final class Pricing {
    private Pricing() {}

    /** Alta temporada: 15/12 até o fim de fevereiro, e julho inteiro. */
    public static boolean isHighSeason(LocalDate night) {
        Month m = night.getMonth();
        return m == Month.JANUARY || m == Month.FEBRUARY || m == Month.JULY
                || (m == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(BigDecimal dailyRate, LocalDate night) {
        return isHighSeason(night) ? Money.percent(dailyRate, 120) : Money.round(dailyRate);
    }

    public static BigDecimal nightsSum(BigDecimal dailyRate, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal sum = Money.ZERO;
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            sum = Money.round(sum.add(nightPrice(dailyRate, d)));
        }
        return sum;
    }

    public static RatePlan.Price price(RatePlan plan, RatePlan.Booking booking, BigDecimal dailyRate) {
        BigDecimal sum = nightsSum(dailyRate, booking.checkIn(), booking.checkOut());
        return plan.price(booking, sum);
    }

}
