package com.pousada.marealta.api;

import java.math.BigDecimal;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.Room;

public class ReceiptResponse {
    public String title;
    public long reservationId;
    public String guestName;
    public String guestCpf;
    public int roomNumber;
    public String roomType;
    public String checkIn;
    public String checkOut;
    public int nights;
    public BigDecimal subtotal;
    public BigDecimal discount;
    public BigDecimal total;

    public ReceiptResponse(Reservation reservation, Room room) {
        this.title = "Comprovante de reserva";
        this.reservationId = reservation.id;
        this.guestName = reservation.guestName;
        this.guestCpf = reservation.guestCpf;
        this.roomNumber = reservation.roomNumber;
        this.roomType = room.type.name;
        this.checkIn = reservation.checkIn.toString();
        this.checkOut = reservation.checkOut.toString();
        this.nights = reservation.nights;
        this.subtotal = reservation.subtotal;
        this.discount = reservation.discount;
        this.total = reservation.total;
    }
}
