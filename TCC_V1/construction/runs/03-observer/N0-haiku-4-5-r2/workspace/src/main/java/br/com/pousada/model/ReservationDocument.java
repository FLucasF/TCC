package br.com.pousada.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public class ReservationDocument {
    @JsonProperty("title")
    public String title;

    @JsonProperty("reservationId")
    public long reservationId;

    @JsonProperty("guestName")
    public String guestName;

    @JsonProperty("guestCpf")
    public String guestCpf;

    @JsonProperty("roomNumber")
    public int roomNumber;

    @JsonProperty("roomType")
    public String roomType;

    @JsonProperty("checkIn")
    public LocalDate checkIn;

    @JsonProperty("checkOut")
    public LocalDate checkOut;

    @JsonProperty("nights")
    public long nights;

    @JsonProperty("subtotal")
    public double subtotal;

    @JsonProperty("discount")
    public double discount;

    @JsonProperty("total")
    public double total;

    public ReservationDocument(long reservationId, String guestName, String guestCpf,
                               int roomNumber, String roomType, LocalDate checkIn, LocalDate checkOut,
                               long nights, double subtotal, double discount, double total) {
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
