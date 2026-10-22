package br.com.marealta.web.dto;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;

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
    public static ReservationDTO from(Reservation reservation) {
        Money subtotal = reservation.calculateSubtotal();
        Money discount = reservation.calculateDiscount();
        Money total = reservation.calculateTotal();

        return new ReservationDTO(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            reservation.getGuestCpf(),
            reservation.getDateRange().checkIn().toString(),
            reservation.getDateRange().checkOut().toString(),
            reservation.getGuests(),
            reservation.getRatePlanType(),
            reservation.getDateRange().nights(),
            subtotal.amount(),
            discount.amount(),
            total.amount(),
            reservation.getStatus().name(),
            reservation.getAmountPaid().amount(),
            reservation.getRefund() != null ? reservation.getRefund().amount() : null
        );
    }
}
