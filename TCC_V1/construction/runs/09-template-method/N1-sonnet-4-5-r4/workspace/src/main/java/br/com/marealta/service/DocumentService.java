package br.com.marealta.service;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class DocumentService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String generateReceiptText(Reservation reservation) {
        StringBuilder receipt = new StringBuilder();

        receipt.append(String.format("Comprovante de reserva nº %d%n", reservation.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)%n",
            reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));

        String roomType = getRoomType(reservation.getRoomNumber());
        receipt.append(String.format("Quarto: %s (%s)%n",
            reservation.getRoomNumber(), roomType));

        receipt.append(String.format("Entrada: %s | Saída: %s | %d %s%n",
            reservation.getCheckIn().format(DATE_FORMATTER),
            reservation.getCheckOut().format(DATE_FORMATTER),
            reservation.getNights(),
            reservation.getNights() == 1 ? "diária" : "diárias"));

        receipt.append(String.format("Diárias: %s%n", formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: %s%n", formatMoney(reservation.getDiscount())));
        }

        receipt.append(String.format("Total: %s", formatMoney(reservation.getTotal())));

        return receipt.toString();
    }

    private String getRoomType(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9, 11));
        }
        return cpf;
    }

    private String formatMoney(BigDecimal value) {
        return String.format("R$ %,.2f", value).replace(",", ".");
    }
}
