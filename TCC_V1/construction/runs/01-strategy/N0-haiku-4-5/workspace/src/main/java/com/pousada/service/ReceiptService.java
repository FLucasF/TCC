package com.pousada.service;

import com.pousada.db.Receipt;
import com.pousada.db.Reservation;
import com.pousada.db.Room;
import com.pousada.util.DateFormatter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class ReceiptService {
    public static Receipt createReceipt(Reservation reservation) throws SQLException {
        Room room = RoomService.getRoomByNumber(reservation.roomNumber);
        return new Receipt(
            reservation.id,
            reservation.guestName,
            reservation.guestCpf,
            reservation.roomNumber,
            room.type,
            reservation.checkIn,
            reservation.checkOut,
            reservation.nights,
            reservation.subtotal,
            reservation.discount,
            reservation.total
        );
    }

    public static String formatReceipt(Receipt receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(receipt.reservationId).append("\n");
        sb.append("Hóspede: ").append(receipt.guestName).append(" (CPF ").append(receipt.guestCpf).append(")\n");
        sb.append("Quarto: ").append(receipt.roomNumber).append(" (").append(receipt.roomType).append(")\n");
        sb.append("Entrada: ").append(DateFormatter.formatDisplay(receipt.checkIn))
          .append(" | Saída: ").append(DateFormatter.formatDisplay(receipt.checkOut))
          .append(" | ").append(receipt.nights).append(receipt.nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatCurrency(receipt.subtotal)).append("\n");

        if (receipt.discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(receipt.discount)).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(receipt.total)).append("\n");

        return sb.toString();
    }

    public static String createConfirmationEmail(String guestName, Receipt receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(guestName).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(formatReceipt(receipt));
        sb.append("\nA entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!\n");

        return sb.toString();
    }

    public static String formatCurrency(BigDecimal amount) {
        String formatted = String.format("%.2f", amount);
        String[] parts = formatted.split("\\.");
        StringBuilder result = new StringBuilder();
        int len = parts[0].length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                result.append(".");
            }
            result.append(parts[0].charAt(i));
        }
        return "R$ " + result.toString() + "," + parts[1];
    }
}
