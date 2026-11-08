package com.pousada.dto;

import java.time.LocalDate;

public class CreateReservationRequest {
    public int roomNumber;
    public String guestName;
    public String guestEmail;
    public String guestCpf;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public int guests;
    public String ratePlan;
}
