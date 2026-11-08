package pousada.plan;

import pousada.Money;
import pousada.Room;
import pousada.Season;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Tudo o que um plano de tarifa pode precisar para decidir se aceita a reserva e quanto ela custa. */
public record BookingContext(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
    private static final BigDecimal HIGH_SEASON = new BigDecimal("1.20");

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Soma das noites ao preço de tabela (com a alta temporada), cada noite arredondada. */
    public BigDecimal seasonalSubtotal() {
        BigDecimal sum = Money.ZERO;
        for (LocalDate d = checkIn; d.isBefore(checkOut); d = d.plusDays(1)) {
            BigDecimal night = Season.isHigh(d) ? Money.times(room.nightlyRate(), HIGH_SEASON) : room.nightlyRate();
            sum = sum.add(night);
        }
        return Money.round(sum);
    }
}
