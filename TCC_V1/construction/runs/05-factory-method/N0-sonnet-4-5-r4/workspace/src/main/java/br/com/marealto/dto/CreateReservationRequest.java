package br.com.marealto.dto;

import br.com.marealto.model.GuestIdentification;
import br.com.marealto.model.RatePlan;
import br.com.marealto.model.Reservation;
import br.com.marealto.service.ReservationService;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record CreateReservationRequest(
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
        @JsonProperty("ratePlan") RatePlan ratePlan
) {
    public Reservation toReservation() {
        GuestIdentification identification = parseIdentification();

        Reservation reservation = new Reservation();
        reservation.setRoomNumber(roomNumber);
        reservation.setGuestName(guestName);
        reservation.setGuestEmail(guestEmail);
        reservation.setGuestIdentification(identification);
        reservation.setCheckIn(checkIn);
        reservation.setCheckOut(checkOut);
        reservation.setGuests(guests);
        reservation.setRatePlan(ratePlan);

        return reservation;
    }

    private GuestIdentification parseIdentification() {
        int count = 0;
        GuestIdentification result = null;

        if (guestCpf != null) {
            count++;
            result = new GuestIdentification.Individual(guestCpf);
        }

        if (companyCnpj != null && companyName != null) {
            count++;
            result = new GuestIdentification.Company(companyCnpj, companyName);
        } else if (companyCnpj != null || companyName != null) {
            throw new ReservationService.InvalidRequestException("Both companyCnpj and companyName are required");
        }

        if (guestPassport != null && guestCountry != null) {
            count++;
            result = new GuestIdentification.Foreign(guestPassport, guestCountry);
        } else if (guestPassport != null || guestCountry != null) {
            throw new ReservationService.InvalidRequestException("Both guestPassport and guestCountry are required");
        }

        if (count != 1) {
            throw new ReservationService.InvalidRequestException("Exactly one identification method is required");
        }

        return result;
    }

    public void validate() {
        if (roomNumber == null || guestName == null || guestEmail == null ||
            checkIn == null || checkOut == null || ratePlan == null) {
            throw new ReservationService.InvalidRequestException("Missing required fields");
        }
    }
}
