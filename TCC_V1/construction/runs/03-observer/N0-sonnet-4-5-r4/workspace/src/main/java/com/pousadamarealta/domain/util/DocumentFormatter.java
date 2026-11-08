package com.pousadamarealta.domain.util;

import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.RoomType;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;

public class DocumentFormatter {
    public static String formatReceipt(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d%n", reservation.getId()));
        sb.append(String.format("Hóspede: %s (CPF %s)%n",
            reservation.getGuestName(),
            CpfFormatter.format(reservation.getGuestCpf())));

        RoomType roomType = RoomRepository.getRoomType(reservation.getRoomNumber());
        sb.append(String.format("Quarto: %s (%s)%n",
            reservation.getRoomNumber(),
            roomType.name()));

        sb.append(String.format("Entrada: %s | Saída: %s | %d diária%s%n",
            DateFormatter.format(reservation.getCheckIn()),
            DateFormatter.format(reservation.getCheckOut()),
            reservation.getNights(),
            reservation.getNights() == 1 ? "" : "s"));

        sb.append(String.format("Diárias: %s%n", MoneyFormatter.format(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s%n", MoneyFormatter.format(reservation.getDiscount())));
        }

        sb.append(String.format("Total: %s", MoneyFormatter.format(reservation.getTotal())));

        return sb.toString();
    }
}
