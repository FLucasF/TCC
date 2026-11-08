package com.pousada;

public class ReservationDocument {
    public String title;
    public int reservationId;
    public String guestName;
    public String guestCpf;
    public int roomNumber;
    public String roomType;
    public String checkIn;
    public String checkOut;
    public int nights;
    public double subtotal;
    public Double discount;
    public double total;

    public ReservationDocument(int reservationId, String guestName, String guestCpf,
                              int roomNumber, String roomType, String checkIn, String checkOut,
                              int nights, double subtotal, Double discount, double total) {
        this.title = "Comprovante de reserva";
        this.reservationId = reservationId;
        this.guestName = guestName;
        this.guestCpf = guestCpf;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
    }
}
