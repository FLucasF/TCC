package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponse(
    long id,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("companyCnpj") String companyCnpj,
    @JsonProperty("companyName") String companyName,
    @JsonProperty("guestPassport") String guestPassport,
    @JsonProperty("guestCountry") String guestCountry,
    @JsonProperty("checkIn") LocalDate checkIn,
    @JsonProperty("checkOut") LocalDate checkOut,
    int guests,
    @JsonProperty("ratePlan") RatePlan ratePlan,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    ReservationStatus status,
    @JsonProperty("amountPaid") BigDecimal amountPaid,
    BigDecimal refund
) {
    public static ReservationResponse from(Reservation reservation, Room room) {
        BigDecimal subtotal = reservation.getSubtotal(room.getNightlyRate());
        BigDecimal discount = reservation.getDiscount(subtotal);
        BigDecimal total = reservation.getTotal(subtotal, discount);

        GuestIdentification id = reservation.getIdentification();
        String guestCpf = id instanceof CpfIdentification ? ((CpfIdentification) id).getCpf() : null;
        String companyCnpj = id instanceof CnpjIdentification ? ((CnpjIdentification) id).getCnpj() : null;
        String companyName = id instanceof CnpjIdentification ? ((CnpjIdentification) id).getCompanyName() : null;
        String guestPassport = id instanceof PassportIdentification ? ((PassportIdentification) id).getPassport() : null;
        String guestCountry = id instanceof PassportIdentification ? ((PassportIdentification) id).getCountry() : null;

        return new ReservationResponse(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            guestCpf,
            companyCnpj,
            companyName,
            guestPassport,
            guestCountry,
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getGuests(),
            reservation.getRatePlan(),
            reservation.getNights(),
            subtotal,
            discount,
            total,
            reservation.getStatus(),
            reservation.getAmountPaid(),
            reservation.getRefund()
        );
    }
}
