package br.com.marealto.dto;

import br.com.marealto.model.GuestIdentification;
import br.com.marealto.model.Reservation;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponse(
        Long id,
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
        @JsonProperty("ratePlan") String ratePlan,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String status,
        @JsonProperty("amountPaid") BigDecimal amountPaid,
        BigDecimal refund
) {
    public static ReservationResponse from(Reservation reservation) {
        GuestIdentification id = reservation.getGuestIdentification();
        String guestCpf = null;
        String companyCnpj = null;
        String companyName = null;
        String guestPassport = null;
        String guestCountry = null;

        if (id instanceof GuestIdentification.Individual individual) {
            guestCpf = individual.cpf();
        } else if (id instanceof GuestIdentification.Company company) {
            companyCnpj = company.cnpj();
            companyName = company.companyName();
        } else if (id instanceof GuestIdentification.Foreign foreign) {
            guestPassport = foreign.passport();
            guestCountry = foreign.country();
        }

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
