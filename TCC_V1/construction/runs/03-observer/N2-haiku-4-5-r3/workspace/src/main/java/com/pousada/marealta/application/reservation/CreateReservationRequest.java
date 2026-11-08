package com.pousada.marealta.application.reservation;

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

    @Override
    public String toString() {
        return "CreateReservationRequest{" +
                "roomNumber=" + roomNumber +
                ", guestName='" + guestName + '\'' +
                ", guestEmail='" + guestEmail + '\'' +
                ", guestCpf='" + guestCpf + '\'' +
                ", checkIn=" + checkIn +
                ", checkOut=" + checkOut +
                ", guests=" + guests +
                ", ratePlan='" + ratePlan + '\'' +
                '}';
    }
}
