package com.pousamarealta.services;

import com.pousamarealta.models.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DocumentService {
    private static final DateTimeFormatter PT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EN_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static Document generateDocument(Reservation reservation, Room room) {
        Document doc = new Document();
        doc.setReservationId(reservation.getId());
        doc.setGuestName(reservation.getGuestName());
        doc.setGuestCpf(reservation.getGuestCpf());
        doc.setCompanyCnpj(reservation.getCompanyCnpj());
        doc.setCompanyName(reservation.getCompanyName());
        doc.setGuestPassport(reservation.getGuestPassport());
        doc.setGuestCountry(reservation.getGuestCountry());
        doc.setRoomNumber(reservation.getRoomNumber());
        doc.setRoomType(room.getType());
        doc.setCheckIn(reservation.getCheckIn().format(PT_FORMAT));
        doc.setCheckOut(reservation.getCheckOut().format(PT_FORMAT));
        doc.setNights(reservation.getNights());
        doc.setSubtotal(reservation.getSubtotal());
        doc.setDiscount(reservation.getDiscount());
        doc.setTotal(reservation.getTotal());

        if (reservation.getGuestCpf() != null) {
            doc.setTitle("Comprovante de reserva");
            doc.setText(generateComprovante(reservation, room));
        } else if (reservation.getCompanyCnpj() != null) {
            doc.setTitle("Fatura de hospedagem");
            doc.setText(generateFatura(reservation, room));
        } else if (reservation.getGuestPassport() != null) {
            doc.setTitle("Booking receipt");
            doc.setText(generateBookingReceipt(reservation, room));
        }

        return doc;
    }

    private static String generateComprovante(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append(" (CPF ").append(reservation.getGuestCpf()).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(PT_FORMAT));
        sb.append(" | Saída: ").append(reservation.getCheckOut().format(PT_FORMAT));
        sb.append(" | ").append(reservation.getNights()).append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.getTotal()));

        return sb.toString();
    }

    private static String generateFatura(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(reservation.getId()).append("\n");
        sb.append("Empresa: ").append(reservation.getCompanyName()).append(" (CNPJ ").append(reservation.getCompanyCnpj()).append(")\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append("\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(PT_FORMAT));
        sb.append(" | Saída: ").append(reservation.getCheckOut().format(PT_FORMAT));
        sb.append(" | ").append(reservation.getNights()).append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(reservation.getTotal())).append("\n");

        BigDecimal iss = PriceService.calculateISS(reservation.getTotal());
        sb.append("ISS retido (5%): ").append(formatMoney(iss));

        return sb.toString();
    }

    private static String generateBookingReceipt(Reservation reservation, Room room) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(reservation.getId()).append("\n");
        sb.append("Guest: ").append(reservation.getGuestName());
        sb.append(" (passport ").append(reservation.getGuestPassport());
        sb.append(", ").append(reservation.getGuestCountry()).append(")\n");
        sb.append("Room: ").append(reservation.getRoomNumber()).append(" (").append(room.getType()).append(")\n");
        sb.append("Check-in: ").append(reservation.getCheckIn().format(EN_FORMAT));
        sb.append(" | Check-out: ").append(reservation.getCheckOut().format(EN_FORMAT));
        sb.append(" | ").append(reservation.getNights()).append(reservation.getNights() == 1 ? " night" : " nights").append("\n");
        sb.append("Nights: ").append(formatMoneyEN(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(formatMoneyEN(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatMoneyEN(reservation.getTotal()));

        return sb.toString();
    }

    private static String formatMoney(BigDecimal value) {
        long cents = Math.round(value.doubleValue() * 100);
        long reais = cents / 100;
        long remainingCents = cents % 100;
        return String.format("R$ %d.%02d", reais, remainingCents);
    }

    private static String formatMoneyEN(BigDecimal value) {
        long cents = Math.round(value.doubleValue() * 100);
        long reais = cents / 100;
        long remainingCents = cents % 100;
        return String.format("BRL %d,%02d", reais, remainingCents);
    }
}
