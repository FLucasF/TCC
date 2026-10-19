package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

/** Preço da estadia: diária do quarto, acréscimo de alta temporada e desconto do plano. */
public final class Pricing {

    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    private Pricing() {
    }

    public record Quote(int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    }

    public static boolean isHighSeason(LocalDate night) {
        Month month = night.getMonth();
        if (month == Month.JANUARY || month == Month.FEBRUARY || month == Month.JULY) {
            return true;
        }
        return month == Month.DECEMBER && night.getDayOfMonth() >= 15;
    }

    public static Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        BigDecimal subtotal = BigDecimal.ZERO;
        int nights = 0;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = Money.round(subtotal.add(nightPrice(room, night)));
            nights++;
        }
        BigDecimal discount = plan.discountOn(subtotal);
        return new Quote(nights, subtotal, discount, Money.round(subtotal.subtract(discount)));
    }

    private static BigDecimal nightPrice(Room room, LocalDate night) {
        if (isHighSeason(night)) {
            return Money.round(room.nightlyRate().multiply(HIGH_SEASON_FACTOR));
        }
        return room.nightlyRate();
    }
}
