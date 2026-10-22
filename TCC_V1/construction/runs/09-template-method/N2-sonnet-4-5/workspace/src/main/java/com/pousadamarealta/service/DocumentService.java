package com.pousadamarealta.service;

import com.pousadamarealta.domain.Document;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public Document createDocument(Reservation reservation, Room room) {
        Document doc = new Document();
        doc.setTitle("Comprovante de reserva");
        doc.setReservationId(reservation.getId());
        doc.setGuestName(reservation.getGuestName());
        doc.setGuestCpf(reservation.getGuestCpf());
        doc.setRoomNumber(reservation.getRoomNumber());
        doc.setRoomType(room.getType());
        doc.setCheckIn(reservation.getCheckIn());
        doc.setCheckOut(reservation.getCheckOut());
        doc.setNights(reservation.getNights());
        doc.setSubtotal(reservation.getSubtotal());
        doc.setDiscount(reservation.getDiscount());
        doc.setTotal(reservation.getTotal());
        return doc;
    }

    public String formatDocument(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(doc.getReservationId()).append("\n");
        sb.append("Hóspede: ").append(doc.getGuestName());
        sb.append(" (CPF ").append(formatCpf(doc.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(doc.getRoomNumber());
        sb.append(" (").append(doc.getRoomType().name()).append(")\n");
        sb.append("Entrada: ").append(doc.getCheckIn().format(DATE_FORMAT));
        sb.append(" | Saída: ").append(doc.getCheckOut().format(DATE_FORMAT));
        sb.append(" | ").append(doc.getNights()).append(doc.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(CURRENCY_FORMAT.format(doc.getSubtotal())).append("\n");

        if (doc.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(CURRENCY_FORMAT.format(doc.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(CURRENCY_FORMAT.format(doc.getTotal()));
        return sb.toString();
    }

    private String formatCpf(String cpf) {
        String digits = cpf.replaceAll("[^0-9]", "");
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "." +
                   digits.substring(3, 6) + "." +
                   digits.substring(6, 9) + "-" +
                   digits.substring(9);
        }
        return cpf;
    }
}
