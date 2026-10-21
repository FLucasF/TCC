package com.pousada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponse(
    @JsonProperty("id") long id,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("companyName") String companyName,
    @JsonProperty("companyCnpj") String companyCnpj,
    @JsonProperty("guestPassport") String guestPassport,
    @JsonProperty("guestCountry") String guestCountry,
    @JsonProperty("checkIn") LocalDate checkIn,
    @JsonProperty("checkOut") LocalDate checkOut,
    @JsonProperty("guests") int guests,
    @JsonProperty("ratePlan") RatePlan ratePlan,
    @JsonProperty("nights") int nights,
    @JsonProperty("subtotal") BigDecimal subtotal,
    @JsonProperty("discount") BigDecimal discount,
    @JsonProperty("total") BigDecimal total,
    @JsonProperty("status") ReservationStatus status,
    @JsonProperty("amountPaid") BigDecimal amountPaid,
    @JsonProperty("refund") BigDecimal refund
) {
    public static ReservationResponse from(Reservation reservation) {
        GuestIdentification identification = reservation.getIdentification();

        String guestCpf = null;
        String companyName = null;
        String companyCnpj = null;
        String guestPassport = null;
        String guestCountry = null;

        if (identification instanceof CpfIdentification cpf) {
            guestCpf = cpf.guestCpf();
        } else if (identification instanceof CnpjIdentification cnpj) {
            companyName = cnpj.companyName();
            companyCnpj = cnpj.companyCnpj();
        } else if (identification instanceof PassportIdentification passport) {
            guestPassport = passport.guestPassport();
            guestCountry = passport.guestCountry();
        }

        return new ReservationResponse(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            guestCpf,
            companyName,
            companyCnpj,
            guestPassport,
            guestCountry,
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getGuests(),
            reservation.getRatePlan(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            reservation.getStatus(),
            reservation.getAmountPaid(),
            reservation.getRefund()
        );
    }
}
