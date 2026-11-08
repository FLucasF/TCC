package pousada.plan;

import pousada.domain.Money;
import pousada.domain.Room;
import pousada.domain.Season;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** O que o plano precisa saber do pedido para validar e calcular o preço. */
public record Stay(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Soma das noites ao preço da diária, com a alta temporada onde ela vale. */
    public BigDecimal seasonalSubtotal() {
        BigDecimal sum = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            sum = sum.add(Season.nightPrice(room.type(), night));
        }
        return Money.round(sum);
    }
}
