package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** O que um plano de tarifa precisa saber da estadia pedida para calcular e validar. */
public record Stay(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Soma das noites ao preço de balcão, com a alta temporada aplicada noite a noite. */
    public BigDecimal nightsTotal() {
        return checkIn.datesUntil(checkOut)
                .map(night -> Season.nightPrice(room, night))
                .reduce(Money.ZERO, BigDecimal::add);
    }
}
