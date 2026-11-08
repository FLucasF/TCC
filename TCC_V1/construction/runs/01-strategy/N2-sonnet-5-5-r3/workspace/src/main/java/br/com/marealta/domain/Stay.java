package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** O que um plano de tarifa precisa saber para precificar e validar uma estadia. */
public record Stay(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
    private static final BigDecimal HIGH_SEASON = new BigDecimal("1.20");

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Soma das noites, cada uma arredondada, com a alta temporada aplicada. */
    public BigDecimal nightsTotal() {
        BigDecimal sum = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            BigDecimal price = Season.isHigh(night)
                    ? Money.round(room.nightlyRate().multiply(HIGH_SEASON))
                    : room.nightlyRate();
            sum = sum.add(price);
        }
        return sum;
    }
}
