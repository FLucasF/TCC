package com.pousada.maralta.controller.dto;

import com.pousada.maralta.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponse(
    long id,
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

        return reservation.getIdentification().accept(new GuestIdentificationVisitor<ReservationResponse>() {
            @Override
            public ReservationResponse visitCpf(CpfIdentification cpf) {
                return new ReservationResponse(
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getGuestName(),
                    reservation.getGuestEmail(),
                    cpf.cpf(),
                    null, null, null, null,
                    reservation.getCheckIn(),
                    reservation.getCheckOut(),
                    reservation.getGuests(),
                    reservation.getRatePlan().name(),
                    reservation.getNights(),
                    reservation.getSubtotal(),
                    reservation.getDiscount(),
                    reservation.getTotal(),
                    reservation.getStatus().name(),
                    reservation.getAmountPaid(),
                    reservation.getRefund()
                );
            }

            @Override
            public ReservationResponse visitCnpj(CnpjIdentification cnpj) {
                return new ReservationResponse(
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getGuestName(),
                    reservation.getGuestEmail(),
                    null,
                    cnpj.cnpj(),
                    cnpj.companyName(),
                    null, null,
                    reservation.getCheckIn(),
                    reservation.getCheckOut(),
                    reservation.getGuests(),
                    reservation.getRatePlan().name(),
                    reservation.getNights(),
                    reservation.getSubtotal(),
                    reservation.getDiscount(),
                    reservation.getTotal(),
                    reservation.getStatus().name(),
                    reservation.getAmountPaid(),
                    reservation.getRefund()
                );
            }

            @Override
            public ReservationResponse visitPassport(PassportIdentification passport) {
                return new ReservationResponse(
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getGuestName(),
                    reservation.getGuestEmail(),
                    null, null, null,
                    passport.passport(),
                    passport.country(),
                    reservation.getCheckIn(),
                    reservation.getCheckOut(),
                    reservation.getGuests(),
                    reservation.getRatePlan().name(),
                    reservation.getNights(),
                    reservation.getSubtotal(),
                    reservation.getDiscount(),
                    reservation.getTotal(),
                    reservation.getStatus().name(),
                    reservation.getAmountPaid(),
                    reservation.getRefund()
                );
            }
        });
    }
}
