package com.pousada.api.dto;

public class CreateReservationRequest {
    public Integer roomNumber;
    public String guestName;
    public String guestEmail;
    public String guestCpf;
    public String checkIn;
    public String checkOut;
    public Integer guests;
    public String ratePlan;
    public String companyCode;

    public CreateReservationRequest() {}

    public CreateReservationRequest(Integer roomNumber, String guestName, String guestEmail,
                                   String guestCpf, String checkIn, String checkOut,
                                   Integer guests, String ratePlan) {
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
    }
}
