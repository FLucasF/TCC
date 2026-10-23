package br.com.pousada.marealta.service;

import br.com.pousada.marealta.model.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {
    private static final NumberFormat BRL_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final NumberFormat USD_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DateTimeFormatter PT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    public Document generateDocument(Reservation reservation, Room room) {
        Document document = new Document();
        document.setReservationId(reservation.getId());
        document.setGuestName(reservation.getGuestName());
        document.setRoomNumber(reservation.getRoomNumber());
        document.setRoomType(room.getType());
        document.setCheckIn(reservation.getCheckIn());
        document.setCheckOut(reservation.getCheckOut());
        document.setNights(reservation.getNights());
        document.setSubtotal(reservation.getSubtotal());
        document.setDiscount(reservation.getDiscount());
        document.setTotal(reservation.getTotal());

        GuestIdentification identification = reservation.getIdentification();

        if (identification instanceof GuestIdentification.Individual individual) {
            document.setTitle("Comprovante de reserva");
            document.setGuestCpf(individual.cpf());
            document.setText(generateReceiptText(document));
        } else if (identification instanceof GuestIdentification.Company company) {
            document.setTitle("Fatura de hospedagem");
            document.setCompanyCnpj(company.cnpj());
            document.setCompanyName(company.companyName());
            document.setText(generateInvoiceText(document));
        } else if (identification instanceof GuestIdentification.Foreign foreign) {
            document.setTitle("Booking receipt");
            document.setGuestPassport(foreign.passport());
            document.setGuestCountry(foreign.country());
            document.setText(generateBookingReceiptText(document));
        }

        return document;
    }

    private String generateReceiptText(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(doc.getReservationId()).append("\n");
        sb.append("Hóspede: ").append(doc.getGuestName())
          .append(" (CPF ").append(formatCpf(doc.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(doc.getRoomNumber())
          .append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(doc.getCheckIn().format(PT_DATE_FORMAT))
          .append(" | Saída: ").append(doc.getCheckOut().format(PT_DATE_FORMAT))
          .append(" | ").append(doc.getNights())
          .append(doc.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(BRL_FORMAT.format(doc.getSubtotal())).append("\n");

        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(BRL_FORMAT.format(doc.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(BRL_FORMAT.format(doc.getTotal()));

        return sb.toString();
    }

    private String generateInvoiceText(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(doc.getReservationId()).append("\n");
        sb.append("Empresa: ").append(doc.getCompanyName())
          .append(" (CNPJ ").append(formatCnpj(doc.getCompanyCnpj())).append(")\n");
        sb.append("Hóspede: ").append(doc.getGuestName()).append("\n");
        sb.append("Quarto: ").append(doc.getRoomNumber())
          .append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(doc.getCheckIn().format(PT_DATE_FORMAT))
          .append(" | Saída: ").append(doc.getCheckOut().format(PT_DATE_FORMAT))
          .append(" | ").append(doc.getNights())
          .append(doc.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(BRL_FORMAT.format(doc.getSubtotal())).append("\n");

        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(BRL_FORMAT.format(doc.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(BRL_FORMAT.format(doc.getTotal())).append("\n");

        BigDecimal iss = doc.getTotal().multiply(ISS_RATE).setScale(2, RoundingMode.HALF_EVEN);
        sb.append("ISS retido (5%): ").append(BRL_FORMAT.format(iss));

        return sb.toString();
    }

    private String generateBookingReceiptText(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(doc.getReservationId()).append("\n");
        sb.append("Guest: ").append(doc.getGuestName())
          .append(" (passport ").append(doc.getGuestPassport())
          .append(", ").append(doc.getGuestCountry()).append(")\n");
        sb.append("Room: ").append(doc.getRoomNumber())
          .append(" (").append(doc.getRoomType()).append(")\n");
        sb.append("Check-in: ").append(doc.getCheckIn().format(EN_DATE_FORMAT))
          .append(" | Check-out: ").append(doc.getCheckOut().format(EN_DATE_FORMAT))
          .append(" | ").append(doc.getNights())
          .append(doc.getNights() == 1 ? " night" : " nights").append("\n");
        sb.append("Nights: ").append(formatBrlForEnglish(doc.getSubtotal())).append("\n");

        if (doc.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(formatBrlForEnglish(doc.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatBrlForEnglish(doc.getTotal()));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private String formatCnpj(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) return cnpj;
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." +
               cnpj.substring(5, 8) + "/" + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
    }

    private String formatBrlForEnglish(BigDecimal value) {
        String formatted = USD_FORMAT.format(value);
        return formatted.replace("$", "BRL ");
    }
}
