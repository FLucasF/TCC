package br.com.marealta;

import br.com.marealta.Domain.RatePlan;
import br.com.marealta.Domain.Room;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;

/** Cálculo de preço. Todo valor é arredondado a centavos (meio para o par) a cada passo. */
public final class Pricing {
    private Pricing() {}

    private static final BigDecimal PEAK_FACTOR = new BigDecimal("1.20");
    private static final BigDecimal NON_REFUNDABLE_DISCOUNT = new BigDecimal("0.15");

    public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}

    /** Alta temporada: de 15/12 ao fim de fevereiro, e julho inteiro. */
    public static boolean isPeakSeason(LocalDate night) {
        Month m = night.getMonth();
        return m == Month.JANUARY || m == Month.FEBRUARY || m == Month.JULY
                || (m == Month.DECEMBER && night.getDayOfMonth() >= 15);
    }

    public static BigDecimal nightPrice(Room room, LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        if (isPeakSeason(night)) {
            rate = rate.multiply(PEAK_FACTOR);
        }
        return money(rate);
    }

    /** As noites são as datas de checkIn (inclusive) a checkOut (exclusive). */
    public static Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2);
        int nights = 0;
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(room, d));
            nights++;
        }
        BigDecimal discount = plan == RatePlan.NON_REFUNDABLE
                ? money(subtotal.multiply(NON_REFUNDABLE_DISCOUNT))
                : money(BigDecimal.ZERO);
        return new Quote(nights, subtotal, discount, money(subtotal.subtract(discount)));
    }

    public static BigDecimal money(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_EVEN);
    }
}
