package br.com.mealta.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public record Receipt(String title, long reservationId, String guestName, String guestCpf, int roomNumber,
                      RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights,
                      BigDecimal subtotal, BigDecimal discount, BigDecimal total) {

    public static final String TITLE = "Comprovante de reserva";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Receipt of(Reservation r, Room room) {
        return new Receipt(TITLE, r.id(), r.guestName(), r.guestCpf(), room.number(), room.type(),
                r.checkInDate(), r.checkOutDate(), r.nights(), r.subtotal(), r.discount(), r.total());
    }

    public String toText() {
        List<String> lines = new ArrayList<>();
        lines.add(title + " nº " + reservationId);
        lines.add("Hóspede: " + guestName + " (CPF " + guestCpf + ")");
        lines.add("Quarto: " + roomNumber + " (" + roomType + ")");
        lines.add("Entrada: " + DATE.format(checkIn) + " | Saída: " + DATE.format(checkOut) + " | "
                + nights + (nights == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + Money.format(subtotal));
        if (discount.signum() > 0) {
            lines.add("Desconto: " + Money.format(discount));
        }
        lines.add("Total: " + Money.format(total));
        return String.join("\n", lines);
    }
}
