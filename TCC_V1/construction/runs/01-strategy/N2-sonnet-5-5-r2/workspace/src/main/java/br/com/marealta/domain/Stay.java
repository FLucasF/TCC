package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** O que um plano de tarifa precisa saber da estadia pedida para calcular e validar. */
public record Stay(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Soma das noites a preço de tabela (com alta temporada), cada noite já arredondada. */
    public BigDecimal sumOfNights() {
        BigDecimal sum = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            sum = sum.add(room.priceOfNight(night));
        }
        return Money.round(sum);
    }
}
