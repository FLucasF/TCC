package pousada;

import java.time.LocalDate;

/** Os dados da reserva que todo documento mostra, qualquer que seja a identificação do hóspede. */
public record DocumentData(int reservationId, String guestName, int roomNumber, RoomType roomType,
                           LocalDate checkIn, LocalDate checkOut, Quote quote) {
}
