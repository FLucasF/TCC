package com.marealata.model;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationDocument {
    @SerializedName("title")
    public String title = "Comprovante de reserva";

    @SerializedName("reservationId")
    public Long reservationId;

    @SerializedName("guestName")
    public String guestName;

    @SerializedName("guestCpf")
    public String guestCpf;

    @SerializedName("roomNumber")
    public int roomNumber;

    @SerializedName("roomType")
    public String roomType;

    @SerializedName("checkIn")
    public LocalDate checkIn;

    @SerializedName("checkOut")
    public LocalDate checkOut;

    @SerializedName("nights")
    public int nights;

    @SerializedName("subtotal")
    public BigDecimal subtotal;

    @SerializedName("discount")
    public BigDecimal discount;

    @SerializedName("total")
    public BigDecimal total;

    public ReservationDocument() {}

    public ReservationDocument(Long reservationId, String guestName, String guestCpf,
                               int roomNumber, String roomType, LocalDate checkIn,
                               LocalDate checkOut, int nights, BigDecimal subtotal,
                               BigDecimal discount, BigDecimal total) {
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
