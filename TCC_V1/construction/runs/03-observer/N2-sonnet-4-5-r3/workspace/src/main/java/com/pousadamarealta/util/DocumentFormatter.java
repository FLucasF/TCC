package com.pousadamarealta.util;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.RoomType;

import java.math.BigDecimal;

public final class DocumentFormatter {
    public static String formatReceipt(Reservation reservation, RoomType roomType) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", reservation.id()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n",
            reservation.guestName(),
            Formatter.formatCpf(reservation.guestCpf())
        ));
        sb.append(String.format("Quarto: %d (%s)\n",
            reservation.roomNumber(),
            roomType
        ));
        sb.append(String.format("Entrada: %s | Saída: %s | %d %s\n",
            Formatter.formatDate(reservation.checkIn()),
            Formatter.formatDate(reservation.checkOut()),
            reservation.nights(),
            reservation.nights() == 1 ? "diária" : "diárias"
        ));
        sb.append(String.format("Diárias: %s\n", Formatter.formatMoney(reservation.subtotal())));

        if (reservation.discount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s\n", Formatter.formatMoney(reservation.discount())));
        }

        sb.append(String.format("Total: %s", Formatter.formatMoney(reservation.total())));

        return sb.toString();
    }
}
