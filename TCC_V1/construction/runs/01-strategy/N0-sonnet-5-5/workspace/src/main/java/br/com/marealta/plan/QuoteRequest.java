package br.com.marealta.plan;

import br.com.marealta.model.Room;
import java.time.LocalDate;

public record QuoteRequest(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
    public int nights() { return (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut); }
}
