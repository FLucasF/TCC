package com.pousadamareal.service;

import com.pousadamareal.model.Document;
import com.pousadamareal.model.Reservation;
import com.pousadamareal.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DocumentGenerator {
    private static final DateTimeFormatter DATE_FORMAT_PT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMAT_EN = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static Document generate(Reservation reservation, Room room) {
        String text;
        String title;
        String checkInFormatted;
        String checkOutFormatted;

        if (reservation.guestCpf() != null) {
            text = generateComprovante(reservation, room);
            title = "Comprovante de reserva";
            checkInFormatted = reservation.checkIn().format(DATE_FORMAT_PT);
            checkOutFormatted = reservation.checkOut().format(DATE_FORMAT_PT);
        } else if (reservation.companyCnpj() != null) {
            text = generateFatura(reservation, room);
            title = "Fatura de hospedagem";
            checkInFormatted = reservation.checkIn().format(DATE_FORMAT_PT);
            checkOutFormatted = reservation.checkOut().format(DATE_FORMAT_PT);
        } else {
            text = generateBookingReceipt(reservation, room);
            title = "Booking receipt";
            checkInFormatted = reservation.checkIn().format(DATE_FORMAT_EN);
            checkOutFormatted = reservation.checkOut().format(DATE_FORMAT_EN);
        }

        return new Document(
            title,
            reservation.id(),
            reservation.guestName(),
            reservation.guestCpf(),
            reservation.companyCnpj(),
            reservation.companyName(),
            reservation.guestPassport(),
            reservation.guestCountry(),
            reservation.roomNumber(),
            room.type().name(),
            checkInFormatted,
            checkOutFormatted,
            reservation.nights(),
            reservation.subtotal(),
            reservation.discount(),
            reservation.total(),
            text
        );
    }

    private static String generateComprovante(Reservation reservation, Room room) {
        String nights = reservation.nights() == 1 ? "1 diária" : reservation.nights() + " diárias";
        String cpfFormatted = formatCpf(reservation.guestCpf());

        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.id()).append("\n");
        sb.append("Hóspede: ").append(reservation.guestName()).append(" (CPF ").append(cpfFormatted).append(")\n");
        sb.append("Quarto: ").append(reservation.roomNumber()).append(" (").append(room.type().name()).append(")\n");
        sb.append("Entrada: ").append(reservation.checkIn().format(DATE_FORMAT_PT));
        sb.append(" | Saída: ").append(reservation.checkOut().format(DATE_FORMAT_PT));
        sb.append(" | ").append(nights).append("\n");
        sb.append("Diárias: ").append(formatMoney(reservation.subtotal())).append("\n");

        if (reservation.discount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.discount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.total()));

        return sb.toString();
    }

    private static String generateFatura(Reservation reservation, Room room) {
        String nights = reservation.nights() == 1 ? "1 diária" : reservation.nights() + " diárias";
        String cnpjFormatted = formatCnpj(reservation.companyCnpj());
        BigDecimal iss = PricingUtils.calculateIss(reservation.total());

        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(reservation.id()).append("\n");
        sb.append("Empresa: ").append(reservation.companyName()).append(" (CNPJ ").append(cnpjFormatted).append(")\n");
        sb.append("Hóspede: ").append(reservation.guestName()).append("\n");
        sb.append("Quarto: ").append(reservation.roomNumber()).append(" (").append(room.type().name()).append(")\n");
        sb.append("Entrada: ").append(reservation.checkIn().format(DATE_FORMAT_PT));
        sb.append(" | Saída: ").append(reservation.checkOut().format(DATE_FORMAT_PT));
        sb.append(" | ").append(nights).append("\n");
        sb.append("Diárias: ").append(formatMoney(reservation.subtotal())).append("\n");

        if (reservation.discount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.discount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.total())).append("\n");
        sb.append("ISS retido (5%): ").append(formatMoney(iss));

        return sb.toString();
    }

    private static String generateBookingReceipt(Reservation reservation, Room room) {
        String nights = reservation.nights() == 1 ? "1 night" : reservation.nights() + " nights";
        String passportCountry = reservation.guestPassport() + ", " + reservation.guestCountry();

        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(reservation.id()).append("\n");
        sb.append("Guest: ").append(reservation.guestName()).append(" (passport ").append(passportCountry).append(")\n");
        sb.append("Room: ").append(reservation.roomNumber()).append(" (").append(room.type().name()).append(")\n");
        sb.append("Check-in: ").append(reservation.checkIn().format(DATE_FORMAT_EN));
        sb.append(" | Check-out: ").append(reservation.checkOut().format(DATE_FORMAT_EN));
        sb.append(" | ").append(nights).append("\n");
        sb.append("Nights: ").append(formatMoneyEn(reservation.subtotal())).append("\n");

        if (reservation.discount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(formatMoneyEn(reservation.discount())).append("\n");
        }

        sb.append("Total: ").append(formatMoneyEn(reservation.total()));

        return sb.toString();
    }

    private static String formatMoney(BigDecimal value) {
        return "R$ " + String.format("%,.2f", value).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    private static String formatMoneyEn(BigDecimal value) {
        return "BRL " + String.format("%,.2f", value);
    }

    private static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private static String formatCnpj(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) return cnpj;
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8) + "/" +
               cnpj.substring(8, 12) + "-" + cnpj.substring(12);
    }
}
