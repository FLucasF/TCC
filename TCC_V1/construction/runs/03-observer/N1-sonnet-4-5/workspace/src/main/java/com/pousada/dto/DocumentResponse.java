package com.pousada.dto;

import com.pousada.domain.*;
import com.pousada.service.RoomService;

public class DocumentResponse {
    private String title;
    private long reservationId;
    private String guestName;
    private String guestCpf;
    private String roomNumber;
    private RoomType roomType;
    private String checkIn;
    private String checkOut;
    private int nights;
    private double subtotal;
    private double discount;
    private double total;

    public static DocumentResponse from(Reservation reservation, RoomService roomService) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        Money subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        Money discount = reservation.calculateDiscount(subtotal);
        Money total = reservation.calculateTotal(subtotal, discount);

        DocumentResponse response = new DocumentResponse();
        response.title = "Comprovante de reserva";
        response.reservationId = reservation.getId();
        response.guestName = reservation.getGuestName();
        response.guestCpf = reservation.getGuestCpf();
        response.roomNumber = room.getNumber();
        response.roomType = room.getType();
        response.checkIn = reservation.getCheckIn().toString();
        response.checkOut = reservation.getCheckOut().toString();
        response.nights = reservation.getNights();
        response.subtotal = subtotal.getAmount().doubleValue();
        response.discount = discount.getAmount().doubleValue();
        response.total = total.getAmount().doubleValue();

        return response;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getReservationId() {
        return reservationId;
    }

    public void setReservationId(long reservationId) {
        this.reservationId = reservationId;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getGuestCpf() {
        return guestCpf;
    }

    public void setGuestCpf(String guestCpf) {
        this.guestCpf = guestCpf;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
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

    public int getNights() {
        return nights;
    }

    public void setNights(int nights) {
        this.nights = nights;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }
}
