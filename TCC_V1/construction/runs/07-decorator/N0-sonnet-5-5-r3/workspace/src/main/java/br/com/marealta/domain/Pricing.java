package br.com.marealta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** Cálculo de preço: tudo arredondado a centavos, meio para o par, a cada passo. */
public final class Pricing {
    private static final BigDecimal HIGH_SEASON = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}

    private Pricing() {}

    public static BigDecimal round(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Alta temporada: 15/12 até o fim de fevereiro, e julho inteiro. */
    public static boolean isHighSeason(LocalDate night) {
        int m = night.getMonthValue();
        return m == 1 || m == 2 || m == 7 || (m == 12 && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(BigDecimal nightlyRate, LocalDate night) {
        return isHighSeason(night) ? round(nightlyRate.multiply(HIGH_SEASON)) : round(nightlyRate);
    }

    public static Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = round(BigDecimal.ZERO);
        int nights = 0;
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            subtotal = round(subtotal.add(nightPrice(room.nightlyRate(), d)));
            nights++;
        }
        BigDecimal discount = plan == RatePlan.NON_REFUNDABLE
                ? round(subtotal.multiply(NON_REFUNDABLE_DISCOUNT)) : round(BigDecimal.ZERO);
        return new Quote(nights, subtotal, discount, round(subtotal.subtract(discount)));
    }
}
