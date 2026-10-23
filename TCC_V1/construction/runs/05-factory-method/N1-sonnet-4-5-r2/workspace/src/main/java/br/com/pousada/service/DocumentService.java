package br.com.pousada.service;

import br.com.pousada.domain.*;
import br.com.pousada.util.PriceCalculator;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {
    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final Locale EN_US = Locale.of("en", "US");
    private static final DateTimeFormatter PT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Document generateDocument(Reservation reservation, Room room) {
        return switch (reservation.getIdentification()) {
            case CpfIdentification cpf -> generateComprovante(reservation, room, cpf);
            case CnpjIdentification cnpj -> generateFatura(reservation, room, cnpj);
            case PassportIdentification passport -> generateReceipt(reservation, room, passport);
        };
    }

    private Document generateComprovante(Reservation reservation, Room room, CpfIdentification cpf) {
        String text = buildComprovante(
            reservation.getId(),
            reservation.getGuestName(),
            cpf.cpf(),
            room.getNumber(),
            room.getType().name(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );

        return new Document(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            cpf.cpf(),
            null,
            null,
            null,
            null,
            room.getNumber(),
            room.getType().name(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            text
        );
    }

    private Document generateFatura(Reservation reservation, Room room, CnpjIdentification cnpj) {
        BigDecimal iss = PriceCalculator.calculateISS(reservation.getTotal());

        String text = buildFatura(
            reservation.getId(),
            cnpj.companyName(),
            cnpj.cnpj(),
            reservation.getGuestName(),
            room.getNumber(),
            room.getType().name(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            iss
        );

        return new Document(
            "Fatura de hospedagem",
            reservation.getId(),
            reservation.getGuestName(),
            null,
            cnpj.cnpj(),
            cnpj.companyName(),
            null,
            null,
            room.getNumber(),
            room.getType().name(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            text
        );
    }

    private Document generateReceipt(Reservation reservation, Room room, PassportIdentification passport) {
        String text = buildReceipt(
            reservation.getId(),
            reservation.getGuestName(),
            passport.passport(),
            passport.country(),
            room.getNumber(),
            room.getType().name(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );

        return new Document(
            "Booking receipt",
            reservation.getId(),
            reservation.getGuestName(),
            null,
            null,
            null,
            passport.passport(),
            passport.country(),
            room.getNumber(),
            room.getType().name(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            text
        );
    }

    private String buildComprovante(Long id, String guestName, String cpf, String roomNumber,
                                   String roomType, java.time.LocalDate checkIn, java.time.LocalDate checkOut,
                                   int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(id).append("\n");
        sb.append("Hóspede: ").append(guestName).append(" (CPF ").append(formatCpf(cpf)).append(")").append("\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")").append("\n");
        sb.append("Entrada: ").append(checkIn.format(PT_DATE_FORMAT));
        sb.append(" | Saída: ").append(checkOut.format(PT_DATE_FORMAT));
        sb.append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(subtotal, PT_BR)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(discount, PT_BR)).append("\n");
        }

        sb.append("Total: ").append(formatMoney(total, PT_BR));

        return sb.toString();
    }

    private String buildFatura(Long id, String companyName, String cnpj, String guestName,
                              String roomNumber, String roomType, java.time.LocalDate checkIn,
                              java.time.LocalDate checkOut, int nights, BigDecimal subtotal,
                              BigDecimal discount, BigDecimal total, BigDecimal iss) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(id).append("\n");
        sb.append("Empresa: ").append(companyName).append(" (CNPJ ").append(formatCnpj(cnpj)).append(")").append("\n");
        sb.append("Hóspede: ").append(guestName).append("\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")").append("\n");
        sb.append("Entrada: ").append(checkIn.format(PT_DATE_FORMAT));
        sb.append(" | Saída: ").append(checkOut.format(PT_DATE_FORMAT));
        sb.append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(subtotal, PT_BR)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(discount, PT_BR)).append("\n");
        }

        sb.append("Total: ").append(formatMoney(total, PT_BR)).append("\n");
        sb.append("ISS retido (5%): ").append(formatMoney(iss, PT_BR));

        return sb.toString();
    }

    private String buildReceipt(Long id, String guestName, String passport, String country,
                               String roomNumber, String roomType, java.time.LocalDate checkIn,
                               java.time.LocalDate checkOut, int nights, BigDecimal subtotal,
                               BigDecimal discount, BigDecimal total) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(id).append("\n");
        sb.append("Guest: ").append(guestName).append(" (passport ").append(passport)
          .append(", ").append(country).append(")").append("\n");
        sb.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")").append("\n");
        sb.append("Check-in: ").append(checkIn.format(EN_DATE_FORMAT));
        sb.append(" | Check-out: ").append(checkOut.format(EN_DATE_FORMAT));
        sb.append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append("\n");
        sb.append("Nights: ").append(formatMoneyEn(subtotal)).append("\n");

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(formatMoneyEn(discount)).append("\n");
        }

        sb.append("Total: ").append(formatMoneyEn(total));

        return sb.toString();
    }

    private String formatMoney(BigDecimal value, Locale locale) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        return formatter.format(value);
    }

    private String formatMoneyEn(BigDecimal value) {
        return String.format("BRL %,.2f", value);
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
        }
        return cpf;
    }

    private String formatCnpj(String cnpj) {
        if (cnpj.length() == 14) {
            return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." +
                   cnpj.substring(5, 8) + "/" + cnpj.substring(8, 12) + "-" + cnpj.substring(12, 14);
        }
        return cnpj;
    }
}
