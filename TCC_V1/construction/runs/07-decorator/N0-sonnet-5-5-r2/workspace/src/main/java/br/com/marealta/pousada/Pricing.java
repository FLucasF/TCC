package br.com.marealta.pousada;

import br.com.marealta.pousada.Domain.RatePlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** Cálculo de diárias, desconto e total. Todo passo é arredondado a centavos (meio para o par). */
public final class Pricing {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    private Pricing() {}

    public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}

    public static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Alta temporada: de 15/12 ao fim de fevereiro, e julho inteiro. */
    public static boolean isHighSeason(LocalDate night) {
        int m = night.getMonthValue();
        return m == 1 || m == 2 || m == 7 || (m == 12 && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(BigDecimal nightlyRate, LocalDate night) {
        BigDecimal base = money(nightlyRate);
        return isHighSeason(night) ? money(base.multiply(HIGH_SEASON_FACTOR)) : base;
    }

    public static Quote quote(BigDecimal nightlyRate, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = money(BigDecimal.ZERO);
        int nights = 0;
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            subtotal = money(subtotal.add(nightPrice(nightlyRate, d)));
            nights++;
        }
        BigDecimal discount = plan == RatePlan.NON_REFUNDABLE
                ? money(subtotal.multiply(NON_REFUNDABLE_DISCOUNT))
                : money(BigDecimal.ZERO);
        return new Quote(nights, subtotal, discount, money(subtotal.subtract(discount)));
    }
}
