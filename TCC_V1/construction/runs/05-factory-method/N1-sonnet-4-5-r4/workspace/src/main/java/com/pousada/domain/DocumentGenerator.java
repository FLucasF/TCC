package com.pousada.domain;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public interface DocumentGenerator {
    String generate(Reservation reservation);

    static DocumentGenerator forIdentification(GuestIdentification identification) {
        return switch (identification) {
            case GuestIdentification.Cpf cpf -> new CpfDocumentGenerator();
            case GuestIdentification.Cnpj cnpj -> new CnpjDocumentGenerator();
            case GuestIdentification.Passport passport -> new PassportDocumentGenerator();
        };
    }
}

class CpfDocumentGenerator implements DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    public String generate(Reservation reservation) {
        GuestIdentification.Cpf cpf = (GuestIdentification.Cpf) reservation.getIdentification();

        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
          .append(" (CPF ").append(cpf.cpf()).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
          .append(" (").append(getRoomTypeName(reservation.getRoomNumber())).append(")\n");
        sb.append("Entrada: ").append(DATE_FORMAT.format(reservation.getCheckIn()))
          .append(" | Saída: ").append(DATE_FORMAT.format(reservation.getCheckOut()))
          .append(" | ").append(formatNights(reservation.getNights())).append("\n");
        sb.append("Diárias: ").append(MONEY_FORMAT.format(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(MONEY_FORMAT.format(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(MONEY_FORMAT.format(reservation.getTotal()));

        return sb.toString();
    }

    private String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private String getRoomTypeName(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }
}

class CnpjDocumentGenerator implements DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final BigDecimal ISS_RATE = new BigDecimal("0.05");

    @Override
    public String generate(Reservation reservation) {
        GuestIdentification.Cnpj cnpj = (GuestIdentification.Cnpj) reservation.getIdentification();

        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(reservation.getId()).append("\n");
        sb.append("Empresa: ").append(cnpj.companyName())
          .append(" (CNPJ ").append(cnpj.cnpj()).append(")\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append("\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
          .append(" (").append(getRoomTypeName(reservation.getRoomNumber())).append(")\n");
        sb.append("Entrada: ").append(DATE_FORMAT.format(reservation.getCheckIn()))
          .append(" | Saída: ").append(DATE_FORMAT.format(reservation.getCheckOut()))
          .append(" | ").append(formatNights(reservation.getNights())).append("\n");
        sb.append("Diárias: ").append(MONEY_FORMAT.format(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(MONEY_FORMAT.format(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(MONEY_FORMAT.format(reservation.getTotal())).append("\n");

        BigDecimal iss = PricingCalculator.round(reservation.getTotal().multiply(ISS_RATE));
        sb.append("ISS retido (5%): ").append(MONEY_FORMAT.format(iss));

        return sb.toString();
    }

    private String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private String getRoomTypeName(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }
}

class PassportDocumentGenerator implements DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String generate(Reservation reservation) {
        GuestIdentification.Passport passport = (GuestIdentification.Passport) reservation.getIdentification();

        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(reservation.getId()).append("\n");
        sb.append("Guest: ").append(reservation.getGuestName())
          .append(" (passport ").append(passport.passport())
          .append(", ").append(passport.country()).append(")\n");
        sb.append("Room: ").append(reservation.getRoomNumber())
          .append(" (").append(getRoomTypeName(reservation.getRoomNumber())).append(")\n");
        sb.append("Check-in: ").append(DATE_FORMAT.format(reservation.getCheckIn()))
          .append(" | Check-out: ").append(DATE_FORMAT.format(reservation.getCheckOut()))
          .append(" | ").append(formatNights(reservation.getNights())).append("\n");
        sb.append("Nights: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.getTotal()));

        return sb.toString();
    }

    private String formatNights(int nights) {
        return nights == 1 ? "1 night" : nights + " nights";
    }

    private String formatMoney(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "BRL " + nf.format(amount);
    }

    private String getRoomTypeName(String roomNumber) {
        return switch (roomNumber) {
            case "101", "102" -> "STANDARD";
            case "201", "202" -> "DELUXE";
            case "301" -> "SUITE";
            default -> "";
        };
    }
}
