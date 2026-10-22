package br.com.marealta.pousada;

import br.com.marealta.pousada.Domain.RatePlan;
import br.com.marealta.pousada.Domain.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class Pricing {
    private Pricing() {}

    private static final BigDecimal HIGH_SEASON = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}

    /** Arredondamento para centavos, meio para o par. */
    public static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Alta temporada: de 15/12 ao fim de fevereiro, e julho inteiro. */
    public static boolean highSeason(LocalDate night) {
        Month m = night.getMonth();
        return m == Month.JANUARY || m == Month.FEBRUARY || m == Month.JULY
                || (m == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return money(highSeason(night) ? rate.multiply(HIGH_SEASON) : rate);
    }

    public static Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = money(BigDecimal.ZERO);
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, d));
        }
        BigDecimal discount = plan == RatePlan.NON_REFUNDABLE
                ? money(subtotal.multiply(NON_REFUNDABLE_DISCOUNT))
                : money(BigDecimal.ZERO);
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        return new Quote(nights, subtotal, discount, money(subtotal.subtract(discount)));
    }

    /** R$ 1.234,56 */
    public static String brl(BigDecimal v) {
        DecimalFormat f = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.of("pt", "BR")));
        f.setRoundingMode(RoundingMode.HALF_EVEN);
        return "R$ " + f.format(v);
    }
}
