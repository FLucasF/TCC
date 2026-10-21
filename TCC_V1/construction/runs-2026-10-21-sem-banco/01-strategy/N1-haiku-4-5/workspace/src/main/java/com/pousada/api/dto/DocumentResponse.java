package com.pousada.api.dto;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import java.time.format.DateTimeFormatter;

public class DocumentResponse {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String title;
    public long reservationId;
    public String guestName;
    public String guestCpf;
    public int roomNumber;
    public String roomType;
    public String checkIn;
    public String checkOut;
    public int nights;
    public double subtotal;
    public double discount;
    public double total;

    public static DocumentResponse fromReservation(Reservation res, Room room) {
        DocumentResponse response = new DocumentResponse();
        response.title = "Comprovante de reserva";
        response.reservationId = res.getId();
        response.guestName = res.getGuestName();
        response.guestCpf = res.getGuestCpf();
        response.roomNumber = res.getRoomNumber();
        response.roomType = room.getType().getName();
        response.checkIn = res.getCheckIn().format(DATE_FORMAT);
        response.checkOut = res.getCheckOut().format(DATE_FORMAT);
        response.nights = res.getNights();
        response.subtotal = res.getSubtotal();
        response.discount = res.getDiscount();
        response.total = res.getTotal();
        return response;
    }
}
