package br.com.mareaalta.domain;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CpfDocumentGenerator implements DocumentGenerator {
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(PT_BR);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void validate(Reservation reservation) {
        if (reservation.getGuestCpf() == null || reservation.getGuestCpf().isBlank()) {
            throw new IllegalArgumentException("CPF is required");
        }
    }

    @Override
    public DocumentData generateDocument(Reservation reservation) {
        DocumentData doc = new DocumentData();
        doc.setTitle("Comprovante de reserva");
        doc.setReservationId(reservation.getId());
        doc.setGuestName(reservation.getGuestName());
        doc.setGuestCpf(reservation.getGuestCpf());
        doc.setCompanyCnpj(null);
        doc.setCompanyName(null);
        doc.setGuestPassport(null);
        doc.setGuestCountry(null);
        doc.setRoomNumber(reservation.getRoomNumber());
        doc.setRoomType(reservation.getRoom().getType());
        doc.setCheckIn(reservation.getCheckIn().format(DATE_FORMAT));
        doc.setCheckOut(reservation.getCheckOut().format(DATE_FORMAT));
        doc.setNights(reservation.getNights());
        doc.setSubtotal(reservation.getSubtotal());
        doc.setDiscount(reservation.getDiscount());
        doc.setTotal(reservation.getTotal());
        doc.setText(generateText(doc));
        return doc;
    }

    private String generateText(DocumentData doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(doc.getReservationId()).append("\n");
        sb.append("Hóspede: ").append(doc.getGuestName())
          .append(" (CPF ").append(formatCpf(doc.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(doc.getRoomNumber())
          .append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(doc.getCheckIn())
          .append(" | Saída: ").append(doc.getCheckOut())
          .append(" | ").append(doc.getNights()).append(doc.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(CURRENCY_FORMAT.format(doc.getSubtotal())).append("\n");

        if (doc.getDiscount() != null && doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(CURRENCY_FORMAT.format(doc.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(CURRENCY_FORMAT.format(doc.getTotal()));
        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }
}
