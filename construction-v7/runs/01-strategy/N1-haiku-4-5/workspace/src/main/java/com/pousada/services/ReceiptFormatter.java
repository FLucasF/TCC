package com.pousada.services;

import com.pousada.models.Reservation;
import com.pousada.models.Receipt;
import com.pousada.models.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReceiptFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMATTER_API = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String formatReceipt(Reservation reservation, Room room) {
        Receipt receipt = createReceipt(reservation, room);
        return buildReceiptText(receipt);
    }

    public static String formatConfirmationEmail(Reservation reservation, Room room, String guestName) {
        Receipt receipt = createReceipt(reservation, room);

        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(guestName).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(buildReceiptText(receipt));
        sb.append("\n\nA entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");

        return sb.toString();
    }

    public static Receipt createReceipt(Reservation reservation, Room room) {
        return new Receipt(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.getType().toString(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    private static String buildReceiptText(Receipt receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(receipt.getReservationId()).append("\n");
        sb.append("Hóspede: ").append(receipt.getGuestName())
          .append(" (CPF ").append(formatCpf(receipt.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(receipt.getRoomNumber())
          .append(" (").append(receipt.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(formatDate(receipt.getCheckIn()))
          .append(" | Saída: ").append(formatDate(receipt.getCheckOut()))
          .append(" | ").append(receipt.getNights()).append(receipt.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(receipt.getSubtotal())).append("\n");

        if (receipt.getDiscount().signum() > 0) {
            sb.append("Desconto: ").append(formatMoney(receipt.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(receipt.getTotal())).append("\n");

        return sb.toString();
    }

    private static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    private static String formatMoney(BigDecimal value) {
        return String.format("R$ %,.2f", value).replace(",", ".");
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9, 11));
    }
}
