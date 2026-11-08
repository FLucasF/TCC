package com.pousada.marealta.infrastructure.web.dto;

import com.pousada.marealta.domain.reservation.Reservation;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReceiptDTO {
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

    public static ReceiptDTO fromDomain(Reservation reservation) {
        ReceiptDTO dto = new ReceiptDTO();
        dto.title = "Comprovante de reserva";
        dto.reservationId = reservation.id();
        dto.guestName = reservation.guest().name();
        dto.guestCpf = reservation.guest().cpf();
        dto.roomNumber = reservation.room().number();
        dto.roomType = reservation.room().type().displayName();
        dto.checkIn = formatDate(reservation.dates().checkIn());
        dto.checkOut = formatDate(reservation.dates().checkOut());
        dto.nights = reservation.dates().nights();
        dto.subtotal = reservation.subtotal().doubleValue();
        dto.discount = reservation.discount().doubleValue();
        dto.total = reservation.total().doubleValue();
        return dto;
    }

    private static String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }
}
