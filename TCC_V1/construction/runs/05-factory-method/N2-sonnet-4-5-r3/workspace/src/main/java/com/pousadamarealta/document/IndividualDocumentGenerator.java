package com.pousadamarealta.document;

import com.pousadamarealta.domain.Document;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class IndividualDocumentGenerator implements DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public Document generate(Reservation reservation, Room room) {
        Document document = new Document();
        document.setTitle("Comprovante de reserva");
        document.setReservationId(reservation.getId());
        document.setGuestName(reservation.getGuestName());
        document.setGuestCpf(reservation.getGuestCpf());
        document.setCompanyCnpj(null);
        document.setCompanyName(null);
        document.setGuestPassport(null);
        document.setGuestCountry(null);
        document.setRoomNumber(room.getNumber());
        document.setRoomType(room.getType());
        document.setCheckIn(reservation.getCheckIn());
        document.setCheckOut(reservation.getCheckOut());
        document.setNights(reservation.getNights());

        BigDecimal subtotal = reservation.getSubtotal(room);
        BigDecimal discount = reservation.getDiscount(subtotal);
        BigDecimal total = reservation.getTotal(room);

        document.setSubtotal(subtotal);
        document.setDiscount(discount);
        document.setTotal(total);
        document.setText(generateText(document));

        return document;
    }

    private String generateText(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(doc.getReservationId()).append("\n");
        sb.append("Hóspede: ").append(doc.getGuestName())
          .append(" (CPF ").append(doc.getGuestCpf()).append(")\n");
        sb.append("Quarto: ").append(doc.getRoomNumber())
          .append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(doc.getCheckIn().format(DATE_FORMATTER))
          .append(" | Saída: ").append(doc.getCheckOut().format(DATE_FORMATTER))
          .append(" | ").append(formatNights(doc.getNights())).append("\n");
        sb.append("Diárias: ").append(formatMoney(doc.getSubtotal())).append("\n");

        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(doc.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(doc.getTotal()));

        return sb.toString();
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(',', '_').replace('.', ',').replace('_', '.');
    }

    private String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }
}
