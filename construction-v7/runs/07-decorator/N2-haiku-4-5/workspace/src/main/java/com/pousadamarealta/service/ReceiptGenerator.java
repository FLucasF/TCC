package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ReceiptGenerator {
    private static final DecimalFormat CURRENCY = new DecimalFormat("0.00");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");

    static {
        CURRENCY.setMinimumFractionDigits(2);
        CURRENCY.setMaximumFractionDigits(2);
    }

    public static String generate(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();

        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
            .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
            .append(" (").append(room.getType().getCode()).append(")\n");
        sb.append("Entrada: ").append(DATE_FORMAT.format(java.sql.Date.valueOf(reservation.getCheckIn())))
            .append(" | Saída: ").append(DATE_FORMAT.format(java.sql.Date.valueOf(reservation.getCheckOut())))
            .append(" | ").append(reservation.getNights()).append(reservation.getNights() == 1 ? " diária" : " diárias")
            .append("\n");
        sb.append("Diárias: R$ ").append(CURRENCY.format(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().signum() > 0) {
            sb.append("Desconto: R$ ").append(CURRENCY.format(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: R$ ").append(CURRENCY.format(reservation.getTotal())).append("\n");

        return sb.toString();
    }

    private static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}
