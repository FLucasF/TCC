package br.com.pousada.marealta.api.dto;

import br.com.pousada.marealta.model.Reservation;
import java.time.LocalDate;

public record ReservationResponse(
        long id,
        int roomNumber,
        String guestName,
        String guestEmail,
        String guestCpf,
        LocalDate checkIn,
        LocalDate checkOut,
        int guests,
        String ratePlan,
        int nights,
        double subtotal,
        double discount,
        double total,
        String status,
        double amountPaid,
        Double refund
) {
    public static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getRoomNumber(),
                reservation.getGuestName(),
                reservation.getGuestEmail(),
                reservation.getGuestCpf(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getGuests(),
                reservation.getRatePlan().toString(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal(),
                reservation.getStatus().toString(),
                reservation.getAmountPaid(),
                reservation.getRefund()
        );
    }
}
