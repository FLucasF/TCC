package com.pousadamarealta.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.model.RatePlan;

public class CreateReservationRequest {
    @JsonProperty("roomNumber")
    private String roomNumber;

    @JsonProperty("guestName")
    private String guestName;

    @JsonProperty("guestEmail")
    private String guestEmail;

    @JsonProperty("guestCpf")
    private String guestCpf;

    @JsonProperty("checkIn")
    private String checkIn;

    @JsonProperty("checkOut")
    private String checkOut;

    @JsonProperty("guests")
    private Integer guests;

    @JsonProperty("ratePlan")
    private RatePlan ratePlan;

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public String getCheckIn() {
        return checkIn;
    }

    public String getCheckOut() {
        return checkOut;
    }

    public Integer getGuests() {
        return guests;
    }

    public RatePlan getRatePlan() {
        return ratePlan;
    }
}
