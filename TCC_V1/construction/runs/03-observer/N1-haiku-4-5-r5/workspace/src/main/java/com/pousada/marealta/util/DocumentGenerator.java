package com.pousada.marealta.util;

import com.pousada.marealta.model.Document;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.db.RoomRepository;
import java.math.BigDecimal;

public class DocumentGenerator {
    public static String generateComprovante(Reservation reservation) throws Exception {
        var room = RoomRepository.findByNumber(reservation.getRoomNumber());

        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
          .append(" (CPF ").append(Formatter.formatCPF(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
          .append(" (").append(room.getType().displayName).append(")\n");
        sb.append("Entrada: ").append(Formatter.formatDate(reservation.getCheckIn()))
          .append(" | Saída: ").append(Formatter.formatDate(reservation.getCheckOut()))
          .append(" | ");

        if (reservation.getNights() == 1) {
            sb.append("1 diária\n");
        } else {
            sb.append(reservation.getNights()).append(" diárias\n");
        }

        sb.append("Diárias: ").append(Formatter.formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(Formatter.formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(Formatter.formatMoney(reservation.getTotal()));

        return sb.toString();
    }

    public static String generateConfirmationEmail(Reservation reservation) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(generateComprovante(reservation)).append("\n\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");

        return sb.toString();
    }
}
