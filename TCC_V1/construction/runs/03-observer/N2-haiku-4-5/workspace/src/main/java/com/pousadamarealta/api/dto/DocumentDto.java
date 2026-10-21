package com.pousadamarealta.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentDto {
    @JsonProperty("title")
    public String title = "Comprovante de reserva";

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
    public String checkIn;

    @JsonProperty("checkOut")
    public String checkOut;

    @JsonProperty("nights")
    public int nights;

    @JsonProperty("subtotal")
    public double subtotal;

    @JsonProperty("discount")
    public double discount;

    @JsonProperty("total")
    public double total;

    public static DocumentDto fromReservation(Reservation reservation, RoomRepository roomRepository) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber());

        DocumentDto dto = new DocumentDto();
        dto.reservationId = reservation.getId();
        dto.guestName = reservation.getGuestName();
        dto.guestCpf = reservation.getGuestCpf();
        dto.roomNumber = reservation.getRoomNumber();
        dto.roomType = room.getType().getName();
        dto.checkIn = reservation.getCheckIn().toString();
        dto.checkOut = reservation.getCheckOut().toString();
        dto.nights = reservation.getNights();
        dto.subtotal = reservation.getSubtotal().doubleValue();
        dto.discount = reservation.getDiscount().doubleValue();
        dto.total = reservation.getTotal().doubleValue();
        return dto;
    }
}
