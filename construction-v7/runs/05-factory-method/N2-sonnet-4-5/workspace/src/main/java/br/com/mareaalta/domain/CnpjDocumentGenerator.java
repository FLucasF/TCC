package br.com.mareaalta.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CnpjDocumentGenerator implements DocumentGenerator {
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(PT_BR);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    public void validate(Reservation reservation) {
        if (reservation.getCompanyCnpj() == null || reservation.getCompanyCnpj().isBlank()) {
            throw new IllegalArgumentException("CNPJ is required");
        }
        if (reservation.getCompanyName() == null || reservation.getCompanyName().isBlank()) {
            throw new IllegalArgumentException("Company name is required");
        }
    }

    @Override
    public DocumentData generateDocument(Reservation reservation) {
        DocumentData doc = new DocumentData();
        doc.setTitle("Fatura de hospedagem");
        doc.setReservationId(reservation.getId());
        doc.setGuestName(reservation.getGuestName());
        doc.setGuestCpf(null);
        doc.setCompanyCnpj(reservation.getCompanyCnpj());
        doc.setCompanyName(reservation.getCompanyName());
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
        sb.append("Fatura de hospedagem nº ").append(doc.getReservationId()).append("\n");
        sb.append("Empresa: ").append(doc.getCompanyName())
          .append(" (CNPJ ").append(formatCnpj(doc.getCompanyCnpj())).append(")\n");
        sb.append("Hóspede: ").append(doc.getGuestName()).append("\n");
        sb.append("Quarto: ").append(doc.getRoomNumber())
          .append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(doc.getCheckIn())
          .append(" | Saída: ").append(doc.getCheckOut())
          .append(" | ").append(doc.getNights()).append(doc.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(CURRENCY_FORMAT.format(doc.getSubtotal())).append("\n");

        if (doc.getDiscount() != null && doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(CURRENCY_FORMAT.format(doc.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(CURRENCY_FORMAT.format(doc.getTotal())).append("\n");

        BigDecimal iss = doc.getTotal().multiply(ISS_RATE).setScale(2, RoundingMode.HALF_EVEN);
        sb.append("ISS retido (5%): ").append(CURRENCY_FORMAT.format(iss));

        return sb.toString();
    }

    private String formatCnpj(String cnpj) {
        if (cnpj.length() == 14) {
            return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." +
                   cnpj.substring(5, 8) + "/" + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
        }
        return cnpj;
    }
}
