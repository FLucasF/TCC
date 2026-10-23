package com.pousada.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.GuestIdentification;
import com.pousada.domain.Reservation;

import java.math.BigDecimal;

public record ReservationDto(
    long id,
    @JsonProperty("roomNumber") String roomNumber,
    @JsonProperty("guestName") String guestName,
    @JsonProperty("guestEmail") String guestEmail,
    @JsonProperty("guestCpf") String guestCpf,
    @JsonProperty("companyCnpj") String companyCnpj,
    @JsonProperty("companyName") String companyName,
    @JsonProperty("guestPassport") String guestPassport,
    @JsonProperty("guestCountry") String guestCountry,
    @JsonProperty("checkIn") String checkIn,
    @JsonProperty("checkOut") String checkOut,
    int guests,
    @JsonProperty("ratePlan") String ratePlan,
    int nights,
    BigDecimal subtotal,
    BigDecimal discount,
    BigDecimal total,
    String status,
    @JsonProperty("amountPaid") BigDecimal amountPaid,
    BigDecimal refund
) {
    public static ReservationDto from(Reservation reservation) {
        GuestIdentification id = reservation.getIdentification();
        String guestCpf = null;
        String companyCnpj = null;
        String companyName = null;
        String guestPassport = null;
        String guestCountry = null;

        if (id instanceof GuestIdentification.Cpf cpf) {
            guestCpf = cpf.cpf();
        } else if (id instanceof GuestIdentification.Cnpj cnpj) {
            companyCnpj = cnpj.cnpj();
            companyName = cnpj.companyName();
        } else if (id instanceof GuestIdentification.Passport passport) {
            guestPassport = passport.passport();
            guestCountry = passport.country();
        }

        return new ReservationDto(
            reservation.getId(),
            reservation.getRoomNumber(),
            reservation.getGuestName(),
            reservation.getGuestEmail(),
            guestCpf,
            companyCnpj,
            companyName,
            guestPassport,
            guestCountry,
            reservation.getCheckIn().toString(),
            reservation.getCheckOut().toString(),
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
}
