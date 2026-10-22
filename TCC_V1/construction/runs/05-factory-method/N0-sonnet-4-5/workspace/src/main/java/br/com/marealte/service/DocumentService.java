package br.com.marealte.service;

import br.com.marealte.model.Document;
import br.com.marealte.model.Reservation;
import br.com.marealte.model.RoomType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {

    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DecimalFormatSymbols BR_SYMBOLS = new DecimalFormatSymbols(new Locale("pt", "BR"));
    private static final DecimalFormatSymbols US_SYMBOLS = new DecimalFormatSymbols(Locale.US);

    public static Document generateDocument(Reservation reservation, RoomType roomType) {
        Document doc = new Document();
        doc.setReservationId(reservation.getId());
        doc.setGuestName(reservation.getGuestName());
        doc.setGuestCpf(reservation.getGuestCpf());
        doc.setCompanyCnpj(reservation.getCompanyCnpj());
        doc.setCompanyName(reservation.getCompanyName());
        doc.setGuestPassport(reservation.getGuestPassport());
        doc.setGuestCountry(reservation.getGuestCountry());
        doc.setRoomNumber(reservation.getRoomNumber());
        doc.setRoomType(roomType);
        doc.setCheckIn(reservation.getCheckIn());
        doc.setCheckOut(reservation.getCheckOut());
        doc.setNights(reservation.getNights());
        doc.setSubtotal(reservation.getSubtotal());
        doc.setDiscount(reservation.getDiscount());
        doc.setTotal(reservation.getTotal());

        if (reservation.getGuestCpf() != null) {
            doc.setTitle("Comprovante de reserva");
            doc.setText(generateComprovante(doc));
        } else if (reservation.getCompanyCnpj() != null) {
            doc.setTitle("Fatura de hospedagem");
            doc.setText(generateFatura(doc));
        } else if (reservation.getGuestPassport() != null) {
            doc.setTitle("Booking receipt");
            doc.setText(generateReceipt(doc));
        }

        return doc;
    }

    private static String generateComprovante(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(doc.getReservationId()).append("\n");
        sb.append("Hóspede: ").append(doc.getGuestName()).append(" (CPF ").append(doc.getGuestCpf()).append(")\n");
        sb.append("Quarto: ").append(doc.getRoomNumber()).append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(doc.getCheckIn().format(BR_DATE));
        sb.append(" | Saída: ").append(doc.getCheckOut().format(BR_DATE));
        sb.append(" | ").append(doc.getNights()).append(doc.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatBRL(doc.getSubtotal())).append("\n");

        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatBRL(doc.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatBRL(doc.getTotal()));

        return sb.toString();
    }

    private static String generateFatura(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(doc.getReservationId()).append("\n");
        sb.append("Empresa: ").append(doc.getCompanyName()).append(" (CNPJ ").append(doc.getCompanyCnpj()).append(")\n");
        sb.append("Hóspede: ").append(doc.getGuestName()).append("\n");
        sb.append("Quarto: ").append(doc.getRoomNumber()).append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(doc.getCheckIn().format(BR_DATE));
        sb.append(" | Saída: ").append(doc.getCheckOut().format(BR_DATE));
        sb.append(" | ").append(doc.getNights()).append(doc.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatBRL(doc.getSubtotal())).append("\n");

        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatBRL(doc.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatBRL(doc.getTotal())).append("\n");

        BigDecimal iss = doc.getTotal().multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
        sb.append("ISS retido (5%): ").append(formatBRL(iss));

        return sb.toString();
    }

    private static String generateReceipt(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(doc.getReservationId()).append("\n");
        sb.append("Guest: ").append(doc.getGuestName()).append(" (passport ").append(doc.getGuestPassport());
        sb.append(", ").append(doc.getGuestCountry()).append(")\n");
        sb.append("Room: ").append(doc.getRoomNumber()).append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Check-in: ").append(doc.getCheckIn().format(ISO_DATE));
        sb.append(" | Check-out: ").append(doc.getCheckOut().format(ISO_DATE));
        sb.append(" | ").append(doc.getNights()).append(doc.getNights() == 1 ? " night" : " nights").append("\n");
        sb.append("Nights: ").append(formatBRLEnglish(doc.getSubtotal())).append("\n");

        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(formatBRLEnglish(doc.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatBRLEnglish(doc.getTotal()));

        return sb.toString();
    }

    private static String formatBRL(BigDecimal value) {
        DecimalFormat df = new DecimalFormat("#,##0.00", BR_SYMBOLS);
        return "R$ " + df.format(value);
    }

    private static String formatBRLEnglish(BigDecimal value) {
        DecimalFormat df = new DecimalFormat("#,##0.00", US_SYMBOLS);
        return "BRL " + df.format(value);
    }
}
