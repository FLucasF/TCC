package br.com.marealta.web.dto;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;

import java.math.BigDecimal;

public record DocumentDTO(
    String title,
    Long reservationId,
    String guestName,
    String guestCpf,
    String roomNumber,
    String roomType,
    String checkIn,
    String checkOut,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total
) {
    public static DocumentDTO from(Reservation reservation) {
        Money subtotal = reservation.calculateSubtotal();
        Money discount = reservation.calculateDiscount();
        Money total = reservation.calculateTotal();

        return new DocumentDTO(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            reservation.getRoomType().name(),
            reservation.getDateRange().checkIn().toString(),
            reservation.getDateRange().checkOut().toString(),
            reservation.getDateRange().nights(),
            subtotal.amount(),
            discount.amount(),
            total.amount()
        );
    }
}
