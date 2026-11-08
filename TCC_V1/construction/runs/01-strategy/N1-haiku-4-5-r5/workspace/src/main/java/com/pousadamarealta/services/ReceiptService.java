package com.pousadamarealta.services;

import com.pousadamarealta.database.RoomRepository;
import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.models.Room;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class ReceiptService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CURRENCY_FORMAT = null;

    public static String generateReceipt(Reservation reservation) throws SQLException {
        Room room = RoomRepository.findById(reservation.roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        StringBuilder receipt = new StringBuilder();
        receipt.append("Comprovante de reserva nº ").append(reservation.id).append("\n");
        receipt.append("Hóspede: ").append(reservation.guestName).append(" (CPF ").append(formatCpf(reservation.guestCpf)).append(")\n");
        receipt.append("Quarto: ").append(reservation.roomNumber).append(" (").append(room.type).append(")\n");
        receipt.append("Entrada: ").append(reservation.checkIn.format(DATE_FORMAT)).append(" | ");
        receipt.append("Saída: ").append(reservation.checkOut.format(DATE_FORMAT)).append(" | ");
        receipt.append(reservation.nights).append(reservation.nights == 1 ? " diária" : " diárias").append("\n");
        receipt.append("Diárias: ").append(formatCurrency(reservation.subtotal)).append("\n");

        if (reservation.discount.compareTo(BigDecimal.ZERO) > 0) {
            receipt.append("Desconto: ").append(formatCurrency(reservation.discount)).append("\n");
        }

        receipt.append("Total: ").append(formatCurrency(reservation.total)).append("\n");

        return receipt.toString();
    }

    private static String formatCurrency(BigDecimal value) {
        return "R$ " + String.format("%.2f", value).replace(".", ",");
    }

    private static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s", cpf.substring(0, 3), cpf.substring(3, 6), cpf.substring(6, 9), cpf.substring(9));
    }
}
