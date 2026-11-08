package com.pousadamareaalta.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Reservation(
    @JsonProperty("id")
    long id,
    @JsonProperty("roomNumber")
    int roomNumber,
    @JsonProperty("guestName")
    String guestName,
    @JsonProperty("guestEmail")
    String guestEmail,
    @JsonProperty("guestCpf")
    String guestCpf,
    @JsonProperty("checkIn")
    LocalDate checkIn,
    @JsonProperty("checkOut")
    LocalDate checkOut,
    @JsonProperty("guests")
    int guests,
    @JsonProperty("ratePlan")
    String ratePlan,
    @JsonProperty("nights")
    int nights,
    @JsonProperty("subtotal")
    double subtotal,
    @JsonProperty("discount")
    double discount,
    @JsonProperty("total")
    double total,
    @JsonProperty("status")
    String status,
    @JsonProperty("amountPaid")
    double amountPaid,
    @JsonProperty("refund")
    Double refund
) {
    public Reservation withStatus(String newStatus) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total,
            newStatus, amountPaid, refund);
    }

    public Reservation withAmountPaid(double newAmountPaid) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total,
            status, newAmountPaid, refund);
    }

    public Reservation withRefund(Double newRefund) {
        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
            checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total,
            status, amountPaid, newRefund);
    }
}
