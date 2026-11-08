package com.pousadamarealta.api.request;

import com.google.gson.annotations.SerializedName;

public class CreateReservationRequest {
    private int roomNumber;
    private String guestName;
    private String guestEmail;
    private String guestCpf;
    private String checkIn;
    private String checkOut;
    private int guests;
    private String ratePlan;
    private String companyCode;

    public int getRoomNumber() { return roomNumber; }
    public String getGuestName() { return guestName; }
    public String getGuestEmail() { return guestEmail; }
    public String getGuestCpf() { return guestCpf; }
    public String getCheckIn() { return checkIn; }
    public String getCheckOut() { return checkOut; }
    public int getGuests() { return guests; }
    public String getRatePlan() { return ratePlan; }
    public String getCompanyCode() { return companyCode; }
}
