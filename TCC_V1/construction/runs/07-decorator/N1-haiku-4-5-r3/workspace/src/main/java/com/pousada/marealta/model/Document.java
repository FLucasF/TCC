package com.pousada.marealta.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Document {
    public final String title;
    public final long reservationId;
    public final String guestName;
    public final String guestCpf;
    public final int roomNumber;
    public final String roomType;
    public final LocalDate checkIn;
    public final LocalDate checkOut;
    public final int nights;
    public final BigDecimal subtotal;
    public final BigDecimal discount;
    public final BigDecimal total;

    public Document(long reservationId, String guestName, String guestCpf, int roomNumber, String roomType,
                    LocalDate checkIn, LocalDate checkOut, int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
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
