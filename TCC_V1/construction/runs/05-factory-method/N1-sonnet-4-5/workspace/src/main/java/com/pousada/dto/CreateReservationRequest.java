package com.pousada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousada.domain.*;

import java.time.LocalDate;

public record CreateReservationRequest(
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
    @JsonProperty("guests") Integer guests,
    @JsonProperty("ratePlan") RatePlan ratePlan
) {
    public GuestIdentification toGuestIdentification() {
        int identificationCount = 0;
        GuestIdentification identification = null;

        if (guestCpf != null) {
            identificationCount++;
            identification = new CpfIdentification(guestName, guestCpf);
        }

        if (companyCnpj != null && companyName != null) {
            identificationCount++;
            identification = new CnpjIdentification(guestName, companyName, companyCnpj);
        }

        if (guestPassport != null && guestCountry != null) {
            identificationCount++;
            identification = new PassportIdentification(guestName, guestPassport, guestCountry);
        }

        if (identificationCount != 1) {
            throw new IllegalArgumentException("Exactly one identification method required");
        }

        return identification;
    }

    public void validate() {
        if (roomNumber == null || guestName == null || guestEmail == null ||
            checkIn == null || checkOut == null || guests == null || ratePlan == null) {
            throw new IllegalArgumentException("Missing required fields");
        }

        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in");
        }

        if (guests < 1) {
            throw new IllegalArgumentException("At least one guest required");
        }

        toGuestIdentification();
    }
}
