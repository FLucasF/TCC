package pousada.identity;

import java.time.LocalDate;

import pousada.Quote;
import pousada.Room;

public record DocumentData(int reservationId, String guestName, Room room,
                           LocalDate checkIn, LocalDate checkOut, Quote quote) {
}
