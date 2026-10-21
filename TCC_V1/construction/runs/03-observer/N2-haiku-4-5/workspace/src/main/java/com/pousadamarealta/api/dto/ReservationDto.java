package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.Reservation;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservationDto {
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
    public String checkIn;

    @JsonProperty("checkOut")
    public String checkOut;

    @JsonProperty("guests")
    public int guests;

    @JsonProperty("ratePlan")
    public String ratePlan;

    @JsonProperty("nights")
    public int nights;

    @JsonProperty("subtotal")
    public double subtotal;

    @JsonProperty("discount")
    public double discount;

    @JsonProperty("total")
    public double total;

    @JsonProperty("status")
    public String status;

    @JsonProperty("amountPaid")
    public double amountPaid;

    @JsonProperty("refund")
    public Double refund;

    public ReservationDto() {}

    public static ReservationDto fromReservation(Reservation reservation) {
        ReservationDto dto = new ReservationDto();
        dto.id = reservation.getId();
        dto.roomNumber = reservation.getRoomNumber();
        dto.guestName = reservation.getGuestName();
        dto.guestEmail = reservation.getGuestEmail();
        dto.guestCpf = reservation.getGuestCpf();
        dto.checkIn = reservation.getCheckIn().toString();
        dto.checkOut = reservation.getCheckOut().toString();
        dto.guests = reservation.getGuests();
        dto.ratePlan = reservation.getRatePlan().toString();
        dto.nights = reservation.getNights();
        dto.subtotal = reservation.getSubtotal().doubleValue();
        dto.discount = reservation.getDiscount().doubleValue();
        dto.total = reservation.getTotal().doubleValue();
        dto.status = reservation.getStatus().toString();
        dto.amountPaid = reservation.getAmountPaid().doubleValue();
        if (reservation.getRefund() != null) {
            dto.refund = reservation.getRefund().doubleValue();
        }
        return dto;
    }
}
