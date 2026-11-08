package pousada.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** O que o hóspede pediu, já com quarto resolvido; é a entrada dos planos de tarifa. */
public record Booking(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Soma das noites ao preço de tabela (com alta temporada), cada noite arredondada. */
    public BigDecimal nightsTotal() {
        return checkIn.datesUntil(checkOut)
                .map(night -> Season.nightPrice(room.nightlyRate(), night))
                .reduce(Money.ZERO, BigDecimal::add);
    }
}
