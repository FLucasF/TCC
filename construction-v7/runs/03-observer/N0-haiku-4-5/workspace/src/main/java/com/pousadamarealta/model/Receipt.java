package com.pousadamarealta.model;

public class Receipt {
    private String title;
    private int reservationId;
    private String guestName;
    private String guestCpf;
    private int roomNumber;
    private String roomType;
    private String checkIn;
    private String checkOut;
    private int nights;
    private double subtotal;
    private double discount;
    private double total;

    public Receipt(int reservationId, String guestName, String guestCpf, int roomNumber, String roomType,
                   String checkIn, String checkOut, int nights, double subtotal, double discount, double total) {
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

    public String getTitle() {
        return title;
    }

    public int getReservationId() {
        return reservationId;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public String getCheckIn() {
        return checkIn;
    }

    public String getCheckOut() {
        return checkOut;
    }

    public int getNights() {
        return nights;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getDiscount() {
        return discount;
    }

    public double getTotal() {
        return total;
    }
}
