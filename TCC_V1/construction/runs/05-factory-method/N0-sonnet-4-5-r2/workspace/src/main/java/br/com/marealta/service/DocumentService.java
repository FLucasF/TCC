package br.com.marealta.service;

import br.com.marealta.model.GuestIdentification;
import br.com.marealta.model.Reservation;
import br.com.marealta.model.Room;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {
    private final PricingService pricingService;
    private static final DateTimeFormatter BR_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    public DocumentService(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    public String generateDocument(Reservation reservation, Room room) {
        return switch (reservation.getGuestIdentification()) {
            case GuestIdentification.IndividualGuest ig -> generateComprovante(reservation, room);
            case GuestIdentification.CorporateGuest cg -> generateFatura(reservation, room, cg);
            case GuestIdentification.ForeignGuest fg -> generateBookingReceipt(reservation, room, fg);
        };
    }

    private String generateComprovante(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName());

        if (reservation.getGuestIdentification() instanceof GuestIdentification.IndividualGuest ig) {
            sb.append(" (CPF ").append(formatCpf(ig.cpf())).append(")");
        }

        sb.append("\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber());
        sb.append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(BR_DATE_FORMAT));
        sb.append(" | Saída: ").append(reservation.getCheckOut().format(BR_DATE_FORMAT));
        sb.append(" | ").append(reservation.getNights());
        sb.append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatBRL(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatBRL(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatBRL(reservation.getTotal()));

        return sb.toString();
    }

    private String generateFatura(Reservation reservation, Room room, GuestIdentification.CorporateGuest corporate) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(reservation.getId()).append("\n");
        sb.append("Empresa: ").append(corporate.companyName());
        sb.append(" (CNPJ ").append(formatCnpj(corporate.cnpj())).append(")\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append("\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber());
        sb.append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(BR_DATE_FORMAT));
        sb.append(" | Saída: ").append(reservation.getCheckOut().format(BR_DATE_FORMAT));
        sb.append(" | ").append(reservation.getNights());
        sb.append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatBRL(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatBRL(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatBRL(reservation.getTotal())).append("\n");

        BigDecimal iss = pricingService.calculateIss(reservation.getTotal());
        sb.append("ISS retido (5%): ").append(formatBRL(iss));

        return sb.toString();
    }

    private String generateBookingReceipt(Reservation reservation, Room room, GuestIdentification.ForeignGuest foreign) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(reservation.getId()).append("\n");
        sb.append("Guest: ").append(reservation.getGuestName());
        sb.append(" (passport ").append(foreign.passport());
        sb.append(", ").append(foreign.country()).append(")\n");
        sb.append("Room: ").append(reservation.getRoomNumber());
        sb.append(" (").append(room.getType()).append(")\n");
        sb.append("Check-in: ").append(reservation.getCheckIn().format(ISO_DATE_FORMAT));
        sb.append(" | Check-out: ").append(reservation.getCheckOut().format(ISO_DATE_FORMAT));
        sb.append(" | ").append(reservation.getNights());
        sb.append(reservation.getNights() == 1 ? " night" : " nights").append("\n");
        sb.append("Nights: ").append(formatUSD(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(formatUSD(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatUSD(reservation.getTotal()));

        return sb.toString();
    }

    private String formatBRL(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return "R$ " + df.format(value);
    }

    private String formatUSD(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return "BRL " + df.format(value);
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatCnpj(String cnpj) {
        if (cnpj.length() == 14) {
            return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." +
                   cnpj.substring(5, 8) + "/" + cnpj.substring(8, 12) + "-" +
                   cnpj.substring(12);
        }
        return cnpj;
    }
}
