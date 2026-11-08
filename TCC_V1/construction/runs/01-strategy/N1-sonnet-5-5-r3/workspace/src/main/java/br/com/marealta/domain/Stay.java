package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** O que está sendo reservado: quarto, datas e (para convênio) o código da empresa. */
public record Stay(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
    private static final BigDecimal HIGH_SEASON_FACTOR = new BigDecimal("1.20");

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Soma das noites; cada noite é arredondada antes de somar. */
    public BigDecimal nightsSubtotal() {
        BigDecimal subtotal = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            subtotal = subtotal.add(nightPrice(night));
        }
        return subtotal;
    }

    private BigDecimal nightPrice(LocalDate night) {
        BigDecimal rate = room.nightlyRate();
        return Season.isHigh(night) ? Money.round(rate.multiply(HIGH_SEASON_FACTOR)) : rate;
    }
}
