package br.com.mareal.plans;

import br.com.mareal.domain.RoomType;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** What a rate plan needs to know to price a booking and decide if it is eligible. */
public record Stay(RoomType roomType, LocalDate checkIn, LocalDate checkOut, String companyCode, LocalDate bookedOn) {
    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
