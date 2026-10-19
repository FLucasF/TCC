package com.pousada.api.dto;

public class CreateReservationRequest {
    public int roomNumber;
    public String guestName;
    public String guestEmail;
    public String guestCpf;
    public String checkIn;
    public String checkOut;
    public int guests;
    public String ratePlan;
    public String companyCode;

    public CreateReservationRequest() {}
}
