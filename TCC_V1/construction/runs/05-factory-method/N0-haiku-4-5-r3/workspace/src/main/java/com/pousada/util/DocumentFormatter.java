package com.pousada.util;

import com.pousada.model.Reservation;
import com.pousada.model.Room;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentFormatter {
    private static final DateTimeFormatter BR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Locale BR_LOCALE = new Locale("pt", "BR");

    public static String formatBRDate(LocalDate date) {
        return date.format(BR_DATE);
    }

    public static String formatISODate(LocalDate date) {
        return date.format(ISO_DATE);
    }

    public static String formatBRMoney(BigDecimal value) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(BR_LOCALE);
        return nf.format(value);
    }

    public static String formatBRLEnglish(BigDecimal value) {
        // BRL 1,234.56 format
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "BRL " + nf.format(value);
    }

    public static String getNightsText(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    public static String getNightsTextEnglish(int nights) {
        return nights == 1 ? "1 night" : nights + " nights";
    }

    public static String formatReceipt(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName());

        if (reservation.getGuestCpf() != null) {
            sb.append(" (CPF ").append(formatCPF(reservation.getGuestCpf())).append(")");
        }
        sb.append("\n");

        sb.append("Quarto: ").append(room.getNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(formatBRDate(reservation.getCheckIn()))
            .append(" | Saída: ").append(formatBRDate(reservation.getCheckOut()))
            .append(" | ").append(getNightsText(reservation.getNights())).append("\n");
        sb.append("Diárias: ").append(formatBRMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatBRMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatBRMoney(reservation.getTotal()));

        return sb.toString();
    }

    public static String formatInvoice(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(reservation.getId()).append("\n");
        sb.append("Empresa: ").append(reservation.getCompanyName())
            .append(" (CNPJ ").append(formatCNPJ(reservation.getCompanyCnpj())).append(")\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append("\n");
        sb.append("Quarto: ").append(room.getNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(formatBRDate(reservation.getCheckIn()))
            .append(" | Saída: ").append(formatBRDate(reservation.getCheckOut()))
            .append(" | ").append(getNightsText(reservation.getNights())).append("\n");
        sb.append("Diárias: ").append(formatBRMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatBRMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatBRMoney(reservation.getTotal())).append("\n");

        BigDecimal iss = MoneyUtil.calculateISS(reservation.getTotal());
        sb.append("ISS retido (5%): ").append(formatBRMoney(iss));

        return sb.toString();
    }

    public static String formatBookingReceipt(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(reservation.getId()).append("\n");
        sb.append("Guest: ").append(reservation.getGuestName());

        if (reservation.getGuestPassport() != null) {
            sb.append(" (passport ").append(reservation.getGuestPassport())
                .append(", ").append(reservation.getGuestCountry()).append(")");
        }
        sb.append("\n");

        sb.append("Room: ").append(room.getNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Check-in: ").append(formatISODate(reservation.getCheckIn()))
            .append(" | Check-out: ").append(formatISODate(reservation.getCheckOut()))
            .append(" | ").append(getNightsTextEnglish(reservation.getNights())).append("\n");
        sb.append("Nights: ").append(formatBRLEnglish(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(formatBRLEnglish(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatBRLEnglish(reservation.getTotal()));

        return sb.toString();
    }

    private static String formatCPF(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "."
            + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private static String formatCNPJ(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return cnpj;
        }
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "."
            + cnpj.substring(5, 8) + "/" + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
    }
}
