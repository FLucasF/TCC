package com.pousadamaralta.api.dto;

import com.pousadamaralta.domain.identification.GuestIdentification;
import com.pousadamaralta.domain.model.Reservation;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponse(
    Long id,
    String roomNumber,
    String guestName,
    String guestEmail,
    String guestCpf,
    String companyCnpj,
    String companyName,
    String guestPassport,
    String guestCountry,
    LocalDate checkIn,
    LocalDate checkOut,
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
    public static ReservationResponse from(Reservation reservation) {
        GuestIdentification id = reservation.getIdentification();

        return new ReservationResponse(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            id.getCpf(),
            id.getCnpj(),
            id.getCompanyName(),
            id.getPassport(),
            id.getCountry(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getGuests(),
            reservation.getRatePlanType().name(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            reservation.getStatus().name(),
            reservation.getAmountPaid(),
            reservation.getRefund()
        );
    }
}
