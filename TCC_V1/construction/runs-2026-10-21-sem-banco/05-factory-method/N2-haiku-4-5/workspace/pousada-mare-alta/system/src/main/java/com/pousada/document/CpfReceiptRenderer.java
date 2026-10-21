package com.pousada.document;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import java.time.format.DateTimeFormatter;

public class CpfReceiptRenderer implements DocumentRenderer {
    private final Room room;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public CpfReceiptRenderer(Room room) {
        this.room = room;
    }

    @Override
    public DocumentContent render(Reservation reservation) {
        var roomType = room.type.name;

        String checkInFormatted = reservation.getCheckIn().format(DATE_FORMAT);
        String checkOutFormatted = reservation.getCheckOut().format(DATE_FORMAT);

        String nightsLabel = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n", reservation.getGuestName(), reservation.getGuestCpf()));
        sb.append(String.format("Quarto: %d (%s)\n", reservation.getRoomNumber(), roomType));
        sb.append(String.format("Entrada: %s | Saída: %s | %s\n", checkInFormatted, checkOutFormatted,
                nightsLabel));
        sb.append(String.format("Diárias: R$ %.2f\n", reservation.getSubtotal()));

        if (reservation.getDiscount() > 0) {
            sb.append(String.format("Desconto: R$ %.2f\n", reservation.getDiscount()));
        }

        sb.append(String.format("Total: R$ %.2f", reservation.getTotal()));

        String text = sb.toString();

        return new DocumentContent(
                "Comprovante de reserva",
                reservation.getId(),
                reservation.getGuestName(),
                reservation.getGuestCpf(),
                null,
                null,
                null,
                null,
                reservation.getRoomNumber(),
                roomType,
                checkInFormatted,
                checkOutFormatted,
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal(),
                text);
    }
}
