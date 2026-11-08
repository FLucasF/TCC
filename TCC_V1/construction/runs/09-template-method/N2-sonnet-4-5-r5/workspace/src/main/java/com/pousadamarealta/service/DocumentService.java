package com.pousadamarealta.service;

import com.pousadamarealta.domain.Document;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public Document generateDocument(Reservation reservation, Room room) {
        return new Document(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.type(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    public String formatDocument(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append(doc.title()).append(" nº ").append(doc.reservationId()).append("\n");
        sb.append("Hóspede: ").append(doc.guestName()).append(" (CPF ").append(formatCpf(doc.guestCpf())).append(")\n");
        sb.append("Quarto: ").append(doc.roomNumber()).append(" (").append(doc.roomType()).append(")\n");
        sb.append("Entrada: ").append(doc.checkIn().format(DATE_FORMATTER));
        sb.append(" | Saída: ").append(doc.checkOut().format(DATE_FORMATTER));
        sb.append(" | ").append(doc.nights()).append(doc.nights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatCurrency(doc.subtotal())).append("\n");

        if (doc.discount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(doc.discount())).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(doc.total()));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatCurrency(BigDecimal amount) {
        return CURRENCY_FORMAT.format(amount);
    }
}
