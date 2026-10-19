package br.com.pousada.marealta.model;

import br.com.pousada.marealta.util.DateUtil;
import java.time.LocalDate;

public record ReservationDocument(
        String title,
        long reservationId,
        String guestName,
        String guestCpf,
        int roomNumber,
        String roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        double subtotal,
        double discount,
        double total
) {
    public static ReservationDocument from(Reservation reservation, RoomType roomType) {
        return new ReservationDocument(
                "Comprovante de reserva",
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                reservation.getRoomNumber(),
                roomType.getDisplayName(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal()
        );
    }
}
