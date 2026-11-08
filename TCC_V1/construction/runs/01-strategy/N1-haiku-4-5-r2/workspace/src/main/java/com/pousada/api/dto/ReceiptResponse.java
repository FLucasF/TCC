package com.pousada.api.dto;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;

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
    public double subtotal;
    public double discount;
    public double total;

    public static ReceiptResponse from(Reservation reservation, Room room) {
        ReceiptResponse dto = new ReceiptResponse();
        dto.title = "Comprovante de reserva";
        dto.reservationId = reservation.getId();
        dto.guestName = reservation.getGuestName();
        dto.guestCpf = reservation.getGuestCpf();
        dto.roomNumber = reservation.getRoomNumber();
        dto.roomType = room.getType().toString();
        dto.checkIn = formatDate(reservation.getCheckIn());
        dto.checkOut = formatDate(reservation.getCheckOut());
        dto.nights = reservation.getNights();
        dto.subtotal = reservation.getSubtotal().doubleValue();
        dto.discount = reservation.getDiscount().doubleValue();
        dto.total = reservation.getTotal().doubleValue();
        return dto;
    }

    private static String formatDate(java.time.LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }
}
