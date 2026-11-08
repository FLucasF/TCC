package pousada.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** O que o hóspede pediu, já sem o plano; é o que cada plano de tarifa recebe para decidir. */
public record Booking(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }
}
