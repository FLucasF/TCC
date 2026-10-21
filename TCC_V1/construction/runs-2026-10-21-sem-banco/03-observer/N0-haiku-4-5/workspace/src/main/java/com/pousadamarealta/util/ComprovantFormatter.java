package com.pousadamarealta.util;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.service.RoomService;
import java.time.format.DateTimeFormatter;

public class ComprovantFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CURRENCY_FORMATTER = null;

    public static String format(Reservation res) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", res.id));
        sb.append(String.format("Hóspede: %s (CPF %s)\n", res.guestName, formatCpf(res.guestCpf)));

        String roomType = getRoomType(res.roomNumber);
        sb.append(String.format("Quarto: %d (%s)\n", res.roomNumber, roomType));

        sb.append(String.format("Entrada: %s | Saída: %s | %s\n",
            res.checkIn.format(DATE_FORMATTER),
            res.checkOut.format(DATE_FORMATTER),
            res.nights == 1 ? "1 diária" : res.nights + " diárias"
        ));

        sb.append(String.format("Diárias: %s\n", formatCurrency(res.subtotal)));

        if (res.discount > 0) {
            sb.append(String.format("Desconto: %s\n", formatCurrency(res.discount)));
        }

        sb.append(String.format("Total: %s", formatCurrency(res.total)));

        return sb.toString();
    }

    private static String formatCurrency(double value) {
        return String.format("R$ %.2f", value).replace(".", ",");
    }

    private static String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private static String getRoomType(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };
    }
}
