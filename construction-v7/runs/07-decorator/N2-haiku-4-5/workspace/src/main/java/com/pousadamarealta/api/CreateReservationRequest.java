package com.pousadamarealta.api;

public class CreateReservationRequest {
    public int roomNumber;
    public String guestName;
    public String guestEmail;
    public String guestCpf;
    public String checkIn;
    public String checkOut;
    public int guests;
    public String ratePlan;

    public CreateReservationRequest() {
    }

    public CreateReservationRequest(int roomNumber, String guestName, String guestEmail,
                                    String guestCpf, String checkIn, String checkOut,
                                    int guests, String ratePlan) {
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
