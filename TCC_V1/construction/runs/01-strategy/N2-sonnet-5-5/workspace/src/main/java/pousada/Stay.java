package pousada;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** What a rate plan needs to know to price and validate a reservation. */
public record Stay(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    /** Sum of the nightly prices, seasonality included. */
    public BigDecimal nightsTotal() {
        BigDecimal sum = Money.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            sum = sum.add(Season.nightPrice(room, night));
        }
        return sum;
    }
}
