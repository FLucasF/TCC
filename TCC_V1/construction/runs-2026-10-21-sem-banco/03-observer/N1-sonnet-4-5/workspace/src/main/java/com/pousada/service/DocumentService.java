package com.pousada.service;

import com.pousada.domain.Money;
import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.util.CpfFormatter;
import com.pousada.util.DateFormatter;

public class DocumentService {

    public String generateReceipt(Reservation reservation, Room room, Money subtotal, Money discount, Money total) {
        StringBuilder receipt = new StringBuilder();

        receipt.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        receipt.append("Hóspede: ").append(reservation.getGuestName())
               .append(" (CPF ").append(CpfFormatter.format(reservation.getGuestCpf())).append(")\n");
        receipt.append("Quarto: ").append(room.getNumber())
               .append(" (").append(room.getType()).append(")\n");
        receipt.append("Entrada: ").append(DateFormatter.formatBR(reservation.getCheckIn()))
               .append(" | Saída: ").append(DateFormatter.formatBR(reservation.getCheckOut()))
               .append(" | ").append(reservation.getNights())
               .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        receipt.append("Diárias: ").append(subtotal.formatBRL()).append("\n");

        if (discount.getAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            receipt.append("Desconto: ").append(discount.formatBRL()).append("\n");
        }

        receipt.append("Total: ").append(total.formatBRL());

        return receipt.toString();
    }
}
