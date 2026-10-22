package pousada.service;

import java.time.LocalDate;
import pousada.domain.Identity;
import pousada.domain.RatePlan;

/** Os dados já validados de um pedido de reserva. */
public record NewReservation(
        int roomNumber,
        String guestName,
        String guestEmail,
        Identity identity,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        RatePlan ratePlan) {
}
