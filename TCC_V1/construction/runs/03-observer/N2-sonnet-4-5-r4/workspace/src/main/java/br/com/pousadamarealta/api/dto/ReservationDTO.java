package br.com.pousadamarealta.api.dto;

import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.domain.Room;

import java.math.BigDecimal;

public record ReservationDTO(
    Long id,
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String checkIn,
    String checkOut,
    int guests,
    String ratePlan,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String status,
    BigDecimal amountPaid,
    BigDecimal refund
) {
    public static ReservationDTO from(Reservation reservation, Room room) {
        BigDecimal subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = subtotal.subtract(discount);

        return new ReservationDTO(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getCheckIn().toString(),
            reservation.getCheckOut().toString(),
            reservation.getGuests(),
            reservation.getRatePlan().name(),
            reservation.getNights(),
            subtotal,
            discount,
            total,
            reservation.getStatus().name(),
            reservation.getAmountPaid(),
            reservation.getRefund()
        );
    }
}
