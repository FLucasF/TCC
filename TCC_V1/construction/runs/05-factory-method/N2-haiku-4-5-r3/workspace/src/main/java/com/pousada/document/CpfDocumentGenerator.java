package com.pousada.document;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.util.Formatter;

public class CpfDocumentGenerator implements DocumentGenerator {
    @Override
    public Document generate(Reservation reservation, Room room) {
        String text = buildText(reservation, room);

        return new Document(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            null,
            null,
            null,
            null,
            reservation.getRoomNumber(),
            room.type().name(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            text
        );
    }

    private String buildText(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName());
        sb.append(" (CPF ").append(Formatter.formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber());
        sb.append(" (").append(room.type().name()).append(")\n");

        String nightsLabel = reservation.getNights() == 1 ? "1 diária" :
                            reservation.getNights() + " diárias";
        sb.append("Entrada: ").append(Formatter.formatBrDate(reservation.getCheckIn()));
        sb.append(" | Saída: ").append(Formatter.formatBrDate(reservation.getCheckOut()));
        sb.append(" | ").append(nightsLabel).append("\n");

        sb.append("Diárias: ").append(Formatter.formatBrCurrency(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().signum() > 0) {
            sb.append("Desconto: ").append(Formatter.formatBrCurrency(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(Formatter.formatBrCurrency(reservation.getTotal()));

        return sb.toString();
    }
}
