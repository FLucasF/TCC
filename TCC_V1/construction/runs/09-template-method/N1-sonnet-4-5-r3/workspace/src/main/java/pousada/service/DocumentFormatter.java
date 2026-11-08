package pousada.service;

import pousada.domain.Reservation;
import pousada.domain.Room;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String formatDocument(Reservation reservation, Room room) {
        StringBuilder doc = new StringBuilder();

        doc.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        doc.append(String.format("Hóspede: %s (CPF %s)\n",
            reservation.getGuestName(),
            formatCpf(reservation.getGuestCpf())));
        doc.append(String.format("Quarto: %s (%s)\n",
            reservation.getRoomNumber(),
            room.getType().name()));
        doc.append(String.format("Entrada: %s | Saída: %s | %s\n",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER),
            formatNights(reservation.getNights())));
        doc.append(String.format("Diárias: %s\n", formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            doc.append(String.format("Desconto: %s\n", formatMoney(reservation.getDiscount())));
        }

        doc.append(String.format("Total: %s", formatMoney(reservation.getTotal())));

        return doc.toString();
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9, 11));
        }
        return cpf;
    }

    private static String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private static String formatMoney(BigDecimal amount) {
        return String.format(Locale.of("pt", "BR"), "R$ %,.2f", amount);
    }
}
