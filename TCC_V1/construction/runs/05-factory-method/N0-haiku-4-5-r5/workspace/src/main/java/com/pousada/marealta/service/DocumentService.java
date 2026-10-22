package com.pousada.marealta.service;

import com.pousada.marealta.dto.DocumentResponse;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {

    public static DocumentResponse generateDocument(Reservation reservation) throws SQLException {
        DocumentResponse doc = new DocumentResponse();
        doc.reservationId = reservation.getId();
        doc.guestName = reservation.getGuestName();
        doc.guestCpf = reservation.getGuestCpf();
        doc.companyCnpj = reservation.getCompanyCnpj();
        doc.companyName = reservation.getCompanyName();
        doc.guestPassport = reservation.getGuestPassport();
        doc.guestCountry = reservation.getGuestCountry();
        doc.roomNumber = reservation.getRoomNumber();
        doc.roomType = RoomRepository.findByNumber(reservation.getRoomNumber()).getType().name();
        doc.checkIn = reservation.getCheckIn();
        doc.checkOut = reservation.getCheckOut();
        doc.nights = reservation.getNights();
        doc.subtotal = reservation.getSubtotal();
        doc.discount = reservation.getDiscount();
        doc.total = reservation.getTotal();

        if (reservation.getGuestCpf() != null) {
            doc.title = "Comprovante de reserva";
            doc.text = generateComprovante(reservation);
        } else if (reservation.getCompanyCnpj() != null) {
            doc.title = "Fatura de hospedagem";
            doc.text = generateFatura(reservation);
        } else {
            doc.title = "Booking receipt";
            doc.text = generateBookingReceipt(reservation);
        }

        return doc;
    }

    private static String generateComprovante(Reservation r) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(r.getId()).append("\n");
        sb.append("Hóspede: ").append(r.getGuestName()).append(" (CPF ").append(r.getGuestCpf()).append(")\n");
        sb.append("Quarto: ").append(r.getRoomNumber()).append(" (").append(
            RoomRepository.findByNumber(r.getRoomNumber()).getType().name()).append(")\n");
        sb.append("Entrada: ").append(formatDateBr(r.getCheckIn())).append(" | Saída: ")
            .append(formatDateBr(r.getCheckOut())).append(" | ");
        sb.append(r.getNights()).append(r.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatBrl(r.getSubtotal())).append("\n");

        if (r.getDiscount() != null && r.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatBrl(r.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatBrl(r.getTotal())).append("\n");

        return sb.toString();
    }

    private static String generateFatura(Reservation r) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(r.getId()).append("\n");
        sb.append("Empresa: ").append(r.getCompanyName()).append(" (CNPJ ").append(r.getCompanyCnpj()).append(")\n");
        sb.append("Hóspede: ").append(r.getGuestName()).append("\n");
        sb.append("Quarto: ").append(r.getRoomNumber()).append(" (").append(
            RoomRepository.findByNumber(r.getRoomNumber()).getType().name()).append(")\n");
        sb.append("Entrada: ").append(formatDateBr(r.getCheckIn())).append(" | Saída: ")
            .append(formatDateBr(r.getCheckOut())).append(" | ");
        sb.append(r.getNights()).append(r.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatBrl(r.getSubtotal())).append("\n");

        if (r.getDiscount() != null && r.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatBrl(r.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatBrl(r.getTotal())).append("\n");

        BigDecimal issRetention = PricingService.calculateIssRetention(r.getTotal());
        sb.append("ISS retido (5%): ").append(formatBrl(issRetention)).append("\n");

        return sb.toString();
    }

    private static String generateBookingReceipt(Reservation r) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(r.getId()).append("\n");
        sb.append("Guest: ").append(r.getGuestName()).append(" (passport ").append(r.getGuestPassport())
            .append(", ").append(r.getGuestCountry()).append(")\n");
        sb.append("Room: ").append(r.getRoomNumber()).append(" (").append(
            RoomRepository.findByNumber(r.getRoomNumber()).getType().name()).append(")\n");
        sb.append("Check-in: ").append(formatDateEn(r.getCheckIn())).append(" | Check-out: ")
            .append(formatDateEn(r.getCheckOut())).append(" | ");
        sb.append(r.getNights()).append(r.getNights() == 1 ? " night" : " nights").append("\n");
        sb.append("Nights: ").append(formatUsd(r.getSubtotal())).append("\n");

        if (r.getDiscount() != null && r.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(formatUsd(r.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(formatUsd(r.getTotal())).append("\n");

        return sb.toString();
    }

    private static String formatBrl(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        DecimalFormat df = new DecimalFormat("R$ #,##0.00", symbols);
        return df.format(value);
    }

    private static String formatUsd(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("BRL #,##0.00", symbols);
        return df.format(value);
    }

    private static String formatDateBr(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String formatDateEn(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
