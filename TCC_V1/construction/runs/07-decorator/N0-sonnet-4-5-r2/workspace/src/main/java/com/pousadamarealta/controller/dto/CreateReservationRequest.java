package com.pousadamarealta.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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
    private String ratePlan;

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestEmail() {
        return guestEmail;
    }

    public void setGuestEmail(String guestEmail) {
        this.guestEmail = guestEmail;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public void setGuestCpf(String guestCpf) {
        this.guestCpf = guestCpf;
    }

    public String getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(String checkIn) {
        this.checkIn = checkIn;
    }

    public String getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(String checkOut) {
        this.checkOut = checkOut;
    }

    public Integer getGuests() {
        return guests;
    }

    public void setGuests(Integer guests) {
        this.guests = guests;
    }

    public String getRatePlan() {
        return ratePlan;
    }

    public void setRatePlan(String ratePlan) {
        this.ratePlan = ratePlan;
    }
}
