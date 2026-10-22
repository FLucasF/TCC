package br.com.marealta.reservation;

import br.com.marealta.Money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/** Preço das noites: diária do quarto, com acréscimo na alta temporada, e desconto do plano. */
public final class Pricing {

    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    }

    private Pricing() {
    }

    /** Alta temporada: de 15 de dezembro ao fim de fevereiro, e julho inteiro. */
    public static boolean isHighSeason(LocalDate night) {
        Month month = night.getMonth();
        return month == Month.JULY
                || month == Month.JANUARY
                || month == Month.FEBRUARY
                || (month == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(BigDecimal nightlyRate, LocalDate night) {
        return isHighSeason(night) ? Money.round(nightlyRate.multiply(HIGH_SEASON_FACTOR)) : Money.round(nightlyRate);
    }

    public static Quote quote(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = Money.ZERO;
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = Money.round(subtotal.add(nightPrice(nightlyRate, night)));
            nights++;
        }
        BigDecimal discount = plan.discountOn(subtotal);
        return new Pricing.Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }
}
