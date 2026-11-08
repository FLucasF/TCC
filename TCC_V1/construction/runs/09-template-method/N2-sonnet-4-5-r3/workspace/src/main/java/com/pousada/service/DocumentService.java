package com.pousada.service;

import com.pousada.domain.Document;
import com.pousada.domain.Reservation;
import com.pousada.domain.Room;

public class DocumentService {
    public Document createDocument(Reservation reservation, Room room) {
        return new Document(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            room.getNumber(),
            room.getType().name(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    public String formatDocument(Document document) {
        StringBuilder sb = new StringBuilder();
        sb.append(document.getTitle()).append(" nº ").append(document.getReservationId()).append("\n");
        sb.append("Hóspede: ").append(document.getGuestName())
            .append(" (CPF ").append(formatCpf(document.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(document.getRoomNumber())
            .append(" (").append(document.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(formatDate(document.getCheckIn()))
            .append(" | Saída: ").append(formatDate(document.getCheckOut()))
            .append(" | ").append(document.getNights())
            .append(document.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(document.getSubtotal())).append("\n");

        if (document.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(document.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(document.getTotal()));

        return sb.toString();
    }

    public String createConfirmationEmail(String guestName, String documentText) {
        return String.format("Olá, %s!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n%s\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!",
            guestName, documentText);
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatDate(java.time.LocalDate date) {
        return String.format("%02d/%02d/%04d",
            date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(java.math.BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}
