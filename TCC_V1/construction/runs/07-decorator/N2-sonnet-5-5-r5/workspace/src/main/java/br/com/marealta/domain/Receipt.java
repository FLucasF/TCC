package br.com.marealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public record Receipt(
        String title,
        long reservationId,
        String guestName,
        String guestCpf,
        int roomNumber,
        RoomType roomType,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal total) {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Receipt of(Reservation reservation, Room room) {
        return new Receipt("Comprovante de reserva", reservation.id(), reservation.guestName(),
                reservation.guestCpf(), room.number(), room.type(), reservation.checkIn(), reservation.checkOut(),
                reservation.nights(), reservation.subtotal(), reservation.discount(), reservation.total());
    }

    /** The printed receipt. */
    public String toText() {
        List<String> lines = new ArrayList<>();
        lines.add(title + " nº " + reservationId);
        lines.add("Hóspede: " + guestName + " (CPF " + guestCpf + ")");
        lines.add("Quarto: " + roomNumber + " (" + roomType + ")");
        lines.add("Entrada: " + DATE.format(checkIn) + " | Saída: " + DATE.format(checkOut) + " | " + nights
                + (nights == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + Money.brl(subtotal));
        if (discount.signum() > 0) {
            lines.add("Desconto: " + Money.brl(discount));
        }
        lines.add("Total: " + Money.brl(total));
        return String.join("\n", lines);
    }
}
