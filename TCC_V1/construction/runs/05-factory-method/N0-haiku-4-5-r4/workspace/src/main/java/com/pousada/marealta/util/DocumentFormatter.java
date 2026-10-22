package com.pousada.marealta.util;

import com.pousada.marealta.model.Reservation;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentFormatter {
    private static final DateTimeFormatter PT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String formatComprovante(Reservation res, String roomType) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", res.getId()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n", res.getGuestName(), formatCpf(res.getGuestCpf())));
        sb.append(String.format("Quarto: %d (%s)\n", res.getRoomNumber(), roomType));

        String nightsLabel = res.getNights() == 1 ? "1 diária" : res.getNights() + " diárias";
        sb.append(String.format("Entrada: %s | Saída: %s | %s\n",
            res.getCheckIn().format(PT_DATE_FORMAT),
            res.getCheckOut().format(PT_DATE_FORMAT),
            nightsLabel));

        sb.append(String.format("Diárias: %s\n", formatBRL(res.getSubtotal())));

        if (res.getDiscount() != null && res.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s\n", formatBRL(res.getDiscount())));
        }

        sb.append(String.format("Total: %s", formatBRL(res.getTotal())));

        return sb.toString();
    }

    public static String formatFatura(Reservation res, String roomType) {
        BigDecimal iss = res.getTotal().multiply(new BigDecimal("0.05"))
            .setScale(2, java.math.RoundingMode.HALF_EVEN);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Fatura de hospedagem nº %d\n", res.getId()));
        sb.append(String.format("Empresa: %s (CNPJ %s)\n", res.getCompanyName(), formatCnpj(res.getCompanyCnpj())));
        sb.append(String.format("Hóspede: %s\n", res.getGuestName()));
        sb.append(String.format("Quarto: %d (%s)\n", res.getRoomNumber(), roomType));

        String nightsLabel = res.getNights() == 1 ? "1 diária" : res.getNights() + " diárias";
        sb.append(String.format("Entrada: %s | Saída: %s | %s\n",
            res.getCheckIn().format(PT_DATE_FORMAT),
            res.getCheckOut().format(PT_DATE_FORMAT),
            nightsLabel));

        sb.append(String.format("Diárias: %s\n", formatBRL(res.getSubtotal())));

        if (res.getDiscount() != null && res.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s\n", formatBRL(res.getDiscount())));
        }

        sb.append(String.format("Total: %s\n", formatBRL(res.getTotal())));
        sb.append(String.format("ISS retido (5%%): %s", formatBRL(iss)));

        return sb.toString();
    }

    public static String formatBookingReceipt(Reservation res, String roomType) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Booking receipt no. %d\n", res.getId()));
        sb.append(String.format("Guest: %s (passport %s, %s)\n",
            res.getGuestName(), res.getGuestPassport(), res.getGuestCountry()));
        sb.append(String.format("Room: %d (%s)\n", res.getRoomNumber(), roomType));

        String nightsLabel = res.getNights() == 1 ? "1 night" : res.getNights() + " nights";
        sb.append(String.format("Check-in: %s | Check-out: %s | %s\n",
            res.getCheckIn().format(ISO_DATE_FORMAT),
            res.getCheckOut().format(ISO_DATE_FORMAT),
            nightsLabel));

        sb.append(String.format("Nights: %s\n", formatBRLEnglish(res.getSubtotal())));

        if (res.getDiscount() != null && res.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Discount: %s\n", formatBRLEnglish(res.getDiscount())));
        }

        sb.append(String.format("Total: %s", formatBRLEnglish(res.getTotal())));

        return sb.toString();
    }

    private static String formatBRL(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return "R$ " + df.format(value);
    }

    private static String formatBRLEnglish(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return "BRL " + df.format(value);
    }

    private static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9));
    }

    private static String formatCnpj(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) return cnpj;
        return String.format("%s.%s.%s/%s-%s",
            cnpj.substring(0, 2),
            cnpj.substring(2, 5),
            cnpj.substring(5, 8),
            cnpj.substring(8, 12),
            cnpj.substring(12));
    }

    public static String formatDateISO(LocalDate date) {
        return date.format(ISO_DATE_FORMAT);
    }

    public static String formatDatePT(LocalDate date) {
        return date.format(PT_DATE_FORMAT);
    }
}
