package br.com.marealto.dto;

import br.com.marealto.model.GuestIdentification;
import br.com.marealto.model.Reservation;
import br.com.marealto.model.RoomType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DocumentResponse(
        String title,
        @JsonProperty("reservationId") Long reservationId,
        @JsonProperty("guestName") String guestName,
        @JsonProperty("guestCpf") String guestCpf,
        @JsonProperty("companyCnpj") String companyCnpj,
        @JsonProperty("companyName") String companyName,
        @JsonProperty("guestPassport") String guestPassport,
        @JsonProperty("guestCountry") String guestCountry,
        @JsonProperty("roomNumber") String roomNumber,
        @JsonProperty("roomType") String roomType,
        @JsonProperty("checkIn") LocalDate checkIn,
        @JsonProperty("checkOut") LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total,
        String text
) {
    public static DocumentResponse from(Reservation reservation, RoomType roomType, String documentText) {
        GuestIdentification id = reservation.getGuestIdentification();
        String guestCpf = null;
        String companyCnpj = null;
        String companyName = null;
        String guestPassport = null;
        String guestCountry = null;
        String title;

        if (id instanceof GuestIdentification.Individual individual) {
            guestCpf = individual.cpf();
            title = "Comprovante de reserva";
        } else if (id instanceof GuestIdentification.Company company) {
            companyCnpj = company.cnpj();
            companyName = company.companyName();
            title = "Fatura de hospedagem";
        } else if (id instanceof GuestIdentification.Foreign foreign) {
            guestPassport = foreign.passport();
            guestCountry = foreign.country();
            title = "Booking receipt";
        } else {
            throw new IllegalStateException("Unknown identification type");
        }

        return new DocumentResponse(
                title,
                reservation.getId(),
                reservation.getGuestName(),
                guestCpf,
                companyCnpj,
                companyName,
                guestPassport,
                guestCountry,
                reservation.getRoomNumber(),
                roomType.name(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal(),
                documentText
        );
    }
}
