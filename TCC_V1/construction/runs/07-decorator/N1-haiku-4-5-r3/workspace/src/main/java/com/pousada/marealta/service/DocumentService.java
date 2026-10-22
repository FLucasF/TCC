package com.pousada.marealta.service;

import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.util.CpfFormatter;
import com.pousada.marealta.util.DateFormatter;
import com.pousada.marealta.util.MoneyFormatter;

public class DocumentService {
    public static String buildDocument(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.id).append("\n");
        sb.append("Hóspede: ").append(reservation.guestName)
                .append(" (CPF ").append(CpfFormatter.format(reservation.guestCpf)).append(")\n");
        sb.append("Quarto: ").append(reservation.roomNumber)
                .append(" (").append(room.type).append(")\n");
        sb.append("Entrada: ").append(DateFormatter.format(reservation.checkIn))
                .append(" | Saída: ").append(DateFormatter.format(reservation.checkOut))
                .append(" | ");
        if (reservation.nights == 1) {
            sb.append("1 diária\n");
        } else {
            sb.append(reservation.nights).append(" diárias\n");
        }
        sb.append("Diárias: ").append(MoneyFormatter.format(reservation.subtotal)).append("\n");
        if (reservation.discount.signum() > 0) {
            sb.append("Desconto: ").append(MoneyFormatter.format(reservation.discount)).append("\n");
        }
        sb.append("Total: ").append(MoneyFormatter.format(reservation.total)).append("\n");
        return sb.toString().trim();
    }
}
