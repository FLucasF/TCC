package com.pousada.services;

import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.utils.DateUtils;
import java.math.BigDecimal;

public class ReceiptService {
    private final RoomService roomService;

    public ReceiptService(RoomService roomService) {
        this.roomService = roomService;
    }

    public String generateReceipt(Reservation reservation) {
        Room room = roomService.getRoom(reservation.getRoomNumber());
        StringBuilder sb = new StringBuilder();

        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append(" (CPF ").append(reservation.getGuestCpf()).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(DateUtils.formatDate(reservation.getCheckIn()))
                .append(" | Saída: ").append(DateUtils.formatDate(reservation.getCheckOut()))
                .append(" | ");

        if (reservation.getNights() == 1) {
            sb.append("1 diária\n");
        } else {
            sb.append(reservation.getNights()).append(" diárias\n");
        }

        sb.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().signum() > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.getTotal()));

        return sb.toString();
    }

    public String generateReceiptForEmail(Reservation reservation) {
        return generateReceipt(reservation);
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %s", String.format("%.2f", amount).replace(".", ","));
    }
}
