package com.pousada.plan;

import com.pousada.domain.Money;
import com.pousada.domain.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** O que um plano precisa saber da reserva para validar e calcular o preço. */
public record PricingRequest(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Soma das diárias do quarto, cada noite já arredondada e com a temporada aplicada. */
    public BigDecimal sumOfNights() {
        BigDecimal sum = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            sum = sum.add(room.priceOfNight(night));
        }
        return sum;
    }
}
