package br.com.pousada.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

public class Reservation {
    @JsonProperty("id")
    public long id;

    @JsonProperty("roomNumber")
    public int roomNumber;

    @JsonProperty("guestName")
    public String guestName;

    @JsonProperty("guestEmail")
    public String guestEmail;

    @JsonProperty("guestCpf")
    public String guestCpf;

    @JsonProperty("checkIn")
    public LocalDate checkIn;

    @JsonProperty("checkOut")
    public LocalDate checkOut;

    @JsonProperty("guests")
    public int guests;

    @JsonProperty("ratePlan")
    public RatePlan ratePlan;

    @JsonProperty("nights")
    public long nights;

    @JsonProperty("subtotal")
    public double subtotal;

    @JsonProperty("discount")
    public double discount;

    @JsonProperty("total")
    public double total;

    @JsonProperty("status")
    public ReservationStatus status;

    @JsonProperty("amountPaid")
    public double amountPaid;

    @JsonProperty("refund")
    public Double refund;

    public Reservation() {}

    public Reservation(long id, int roomNumber, String guestName, String guestEmail, String guestCpf,
                       LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan,
                       long nights, double subtotal, double discount, double total,
                       ReservationStatus status, double amountPaid, Double refund) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.guestName = guestName;
        this.guestEmail = guestEmail;
        this.guestCpf = guestCpf;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.guests = guests;
        this.ratePlan = ratePlan;
        this.nights = nights;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.status = status;
        this.amountPaid = amountPaid;
        this.refund = refund;
    }
}
