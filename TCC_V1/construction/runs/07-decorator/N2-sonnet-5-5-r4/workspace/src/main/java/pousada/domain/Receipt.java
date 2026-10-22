package pousada.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

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

    public static Receipt of(Reservation r, Room room) {
        return new Receipt("Comprovante de reserva", r.id(), r.guestName(), r.guestCpf(), room.number(),
                room.type(), r.checkIn(), r.checkOut(), r.nights(), r.subtotal(), r.discount(), r.total());
    }

    public String text() {
        List<String> lines = new ArrayList<>();
        lines.add(title + " nº " + reservationId);
        lines.add("Hóspede: " + guestName + " (CPF " + guestCpf + ")");
        lines.add("Quarto: " + roomNumber + " (" + roomType + ")");
        lines.add("Entrada: " + checkIn.format(DATE) + " | Saída: " + checkOut.format(DATE) + " | "
                + nights + (nights == 1 ? " diária" : " diárias"));
        lines.add("Diárias: " + Money.format(subtotal));
        if (discount.signum() > 0) {
            lines.add("Desconto: " + Money.format(discount));
        }
        lines.add("Total: " + Money.format(total));
        return String.join("\n", lines);
    }
}
