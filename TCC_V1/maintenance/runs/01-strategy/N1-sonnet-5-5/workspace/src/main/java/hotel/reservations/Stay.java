package hotel.reservations;

import hotel.rooms.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What a rate plan needs to know to price and accept a stay: the room, the dates, the company code the
 * guest gave (null if none), and the sum of the nights at the regular, season-adjusted prices.
 */
public record Stay(
        Room room, LocalDate checkIn, LocalDate checkOut, String companyCode, int nights, BigDecimal nightsTotal) {
}
