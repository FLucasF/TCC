package com.pousadamarealta.document;

import com.pousadamarealta.domain.Document;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

public class CorporateDocumentGenerator implements DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    public Document generate(Reservation reservation, Room room) {
        Document document = new Document();
        document.setTitle("Fatura de hospedagem");
        document.setReservationId(reservation.getId());
        document.setGuestName(reservation.getGuestName());
        document.setGuestCpf(null);
        document.setCompanyCnpj(reservation.getCompanyCnpj());
        document.setCompanyName(reservation.getCompanyName());
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
        sb.append("Fatura de hospedagem nº ").append(doc.getReservationId()).append("\n");
        sb.append("Empresa: ").append(doc.getCompanyName())
          .append(" (CNPJ ").append(doc.getCompanyCnpj()).append(")\n");
        sb.append("Hóspede: ").append(doc.getGuestName()).append("\n");
        sb.append("Quarto: ").append(doc.getRoomNumber())
          .append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(doc.getCheckIn().format(DATE_FORMATTER))
          .append(" | Saída: ").append(doc.getCheckOut().format(DATE_FORMATTER))
          .append(" | ").append(formatNights(doc.getNights())).append("\n");
        sb.append("Diárias: ").append(formatMoney(doc.getSubtotal())).append("\n");

        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(doc.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(doc.getTotal())).append("\n");

        BigDecimal iss = doc.getTotal().multiply(ISS_RATE).setScale(2, RoundingMode.HALF_EVEN);
        sb.append("ISS retido (5%): ").append(formatMoney(iss));

        return sb.toString();
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(',', '_').replace('.', ',').replace('_', '.');
    }

    private String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }
}
