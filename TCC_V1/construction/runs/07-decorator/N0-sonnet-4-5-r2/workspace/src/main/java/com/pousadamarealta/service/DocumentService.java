package com.pousadamarealta.service;

import com.pousadamarealta.domain.model.Document;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.domain.repository.RoomRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {

    private final RoomRepository roomRepository;
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public DocumentService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Document generateDocument(Reservation reservation) {
        Document doc = new Document();
        doc.setReservationId(reservation.getId());
        doc.setGuestName(reservation.getGuestName());
        doc.setGuestCpf(reservation.getGuestCpf());
        doc.setRoomNumber(reservation.getRoomNumber());

        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new RuntimeException("Room not found"));
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
        sb.append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(doc.getCheckIn().format(DATE_FORMAT));
        sb.append(" | Saída: ").append(doc.getCheckOut().format(DATE_FORMAT));
        sb.append(" | ").append(doc.getNights());
        sb.append(doc.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatCurrency(doc.getSubtotal())).append("\n");

        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(doc.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(doc.getTotal()));

        return sb.toString();
    }

    public String createConfirmationEmail(Reservation reservation, String documentText) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(documentText).append("\n\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatCurrency(BigDecimal value) {
        return CURRENCY_FORMAT.format(value);
    }
}
