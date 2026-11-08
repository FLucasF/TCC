package com.pousadamarealta.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.models.Reservation;
import java.math.BigDecimal;

public class ReceiptResponse {
    @JsonProperty("title")
    public final String title = "Comprovante de reserva";

    @JsonProperty("reservationId")
    public final int reservationId;

    @JsonProperty("guestName")
    public final String guestName;

    @JsonProperty("guestCpf")
    public final String guestCpf;

    @JsonProperty("roomNumber")
    public final int roomNumber;

    @JsonProperty("roomType")
    public final String roomType;

    @JsonProperty("checkIn")
    public final String checkIn;

    @JsonProperty("checkOut")
    public final String checkOut;

    @JsonProperty("nights")
    public final int nights;

    @JsonProperty("subtotal")
    public final BigDecimal subtotal;

    @JsonProperty("discount")
    public final BigDecimal discount;

    @JsonProperty("total")
    public final BigDecimal total;

    public ReceiptResponse(Reservation reservation, String roomType) {
        this.reservationId = reservation.id;
        this.guestName = reservation.guestName;
        this.guestCpf = formatCpf(reservation.guestCpf);
        this.roomNumber = reservation.roomNumber;
        this.roomType = roomType;
        this.checkIn = reservation.checkIn.toString();
        this.checkOut = reservation.checkOut.toString();
        this.nights = reservation.nights;
        this.subtotal = reservation.subtotal;
        this.discount = reservation.discount;
        this.total = reservation.total;
    }

    private static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s", cpf.substring(0, 3), cpf.substring(3, 6), cpf.substring(6, 9), cpf.substring(9));
    }
}
