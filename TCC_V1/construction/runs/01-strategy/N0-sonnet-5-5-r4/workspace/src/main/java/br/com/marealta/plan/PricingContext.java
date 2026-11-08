package br.com.marealta.plan;

import br.com.marealta.Money;
import br.com.marealta.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Tudo que um plano pode precisar para precificar uma reserva. */
public record PricingContext(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Alta temporada: de 15/12 ao fim de fevereiro e todo julho. */
    public static boolean isHighSeason(LocalDate night) {
        int month = night.getMonthValue();
        return month == 1 || month == 2 || month == 7 || (month == 12 && night.getDayOfMonth() >= 15);
    }

    public BigDecimal nightPrice(LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return isHighSeason(night) ? Money.times(rate, HIGH_SEASON_FACTOR) : Money.round(rate);
    }

    /** Soma das noites a preço de tabela (com alta temporada), cada noite arredondada. */
    public BigDecimal seasonalSubtotal() {
        BigDecimal sum = Money.ZERO;
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            sum = sum.add(nightPrice(d));
        }
        return sum;
    }
}
