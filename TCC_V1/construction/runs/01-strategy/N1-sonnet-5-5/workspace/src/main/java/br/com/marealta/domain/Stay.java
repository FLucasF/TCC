package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** O que o plano de tarifa precisa saber da estadia pedida para calcular o preço e dizer se pode ser usado. */
public record Stay(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Soma das noites, cada uma já arredondada, com a alta temporada aplicada nas noites que forem dela. */
    public BigDecimal nightsTotal() {
        return checkIn.datesUntil(checkOut)
                .map(night -> Season.nightPrice(room, night))
                .reduce(Money.ZERO, BigDecimal::add);
    }
}
