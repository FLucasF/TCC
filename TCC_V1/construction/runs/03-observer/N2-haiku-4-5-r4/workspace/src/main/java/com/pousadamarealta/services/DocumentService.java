package com.pousadamarealta.services;

import com.pousadamarealta.domain.Document;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.utils.Formatting;

public class DocumentService {
    private final Database database;

    public DocumentService(Database database) {
        this.database = database;
    }

    public Document createDocument(Reservation reservation) {
        var room = database.getRoomByNumber(reservation.getRoomNumber()).orElseThrow();
        return new Document(
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.getType(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    public String generateReceiptText(Document document) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(document.getReservationId()).append("\n");
        sb.append("Hóspede: ").append(document.getGuestName()).append(" (CPF ").append(formatCpf(document.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(document.getRoomNumber()).append(" (").append(document.getRoomType().getValue()).append(")\n");
        sb.append("Entrada: ").append(Formatting.formatDate(document.getCheckIn())).append(" | ");
        sb.append("Saída: ").append(Formatting.formatDate(document.getCheckOut())).append(" | ");
        sb.append(getNightsLabel(document.getNights())).append("\n");
        sb.append("Diárias: ").append(Formatting.formatMoney(document.getSubtotal())).append("\n");

        if (document.getDiscount().signum() > 0) {
            sb.append("Desconto: ").append(Formatting.formatMoney(document.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(Formatting.formatMoney(document.getTotal())).append("\n");

        return sb.toString();
    }

    private String getNightsLabel(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }
}
