package com.posadamarealta.service;

import com.posadamarealta.model.Document;
import com.posadamarealta.model.Reservation;

import java.math.BigDecimal;

public class DocumentService {

    public static Document generate(Reservation reservation, String roomType) {
        Document doc = new Document();
        doc.reservationId = reservation.id;
        doc.roomNumber = reservation.roomNumber;
        doc.roomType = roomType;
        doc.guestName = reservation.guestName;
        doc.guestCpf = reservation.guestCpf;
        doc.companyCnpj = reservation.companyCnpj;
        doc.companyName = reservation.companyName;
        doc.guestPassport = reservation.guestPassport;
        doc.guestCountry = reservation.guestCountry;
        doc.checkIn = reservation.checkIn;
        doc.checkOut = reservation.checkOut;
        doc.nights = reservation.nights;
        doc.subtotal = reservation.subtotal;
        doc.discount = reservation.discount;
        doc.total = reservation.total;

        if (reservation.guestCpf != null) {
            doc.title = "Comprovante de reserva";
            doc.text = generateReceipt(doc);
        } else if (reservation.companyCnpj != null) {
            doc.title = "Fatura de hospedagem";
            doc.text = generateInvoice(doc);
        } else if (reservation.guestPassport != null) {
            doc.title = "Booking receipt";
            doc.text = generateBookingReceipt(doc);
        }

        return doc;
    }

    private static String generateReceipt(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(doc.reservationId).append("\n");
        sb.append("Hóspede: ").append(doc.guestName).append(" (CPF ").append(formatCPF(doc.guestCpf)).append(")\n");
        sb.append("Quarto: ").append(doc.roomNumber).append(" (").append(doc.roomType).append(")\n");
        sb.append("Entrada: ").append(UtilService.formatDate(doc.checkIn));
        sb.append(" | Saída: ").append(UtilService.formatDate(doc.checkOut));
        sb.append(" | ").append(doc.nights).append(doc.nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(UtilService.formatMoney(doc.subtotal)).append("\n");

        if (doc.discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(UtilService.formatMoney(doc.discount)).append("\n");
        }

        sb.append("Total: ").append(UtilService.formatMoney(doc.total));
        return sb.toString();
    }

    private static String generateInvoice(Document doc) {
        StringBuilder sb = new StringBuilder();
        BigDecimal iss = UtilService.roundHalfEven(doc.total.multiply(new BigDecimal("0.05")));

        sb.append("Fatura de hospedagem nº ").append(doc.reservationId).append("\n");
        sb.append("Empresa: ").append(doc.companyName).append(" (CNPJ ").append(formatCNPJ(doc.companyCnpj)).append(")\n");
        sb.append("Hóspede: ").append(doc.guestName).append("\n");
        sb.append("Quarto: ").append(doc.roomNumber).append(" (").append(doc.roomType).append(")\n");
        sb.append("Entrada: ").append(UtilService.formatDate(doc.checkIn));
        sb.append(" | Saída: ").append(UtilService.formatDate(doc.checkOut));
        sb.append(" | ").append(doc.nights).append(doc.nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(UtilService.formatMoney(doc.subtotal)).append("\n");

        if (doc.discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(UtilService.formatMoney(doc.discount)).append("\n");
        }

        sb.append("Total: ").append(UtilService.formatMoney(doc.total)).append("\n");
        sb.append("ISS retido (5%): ").append(UtilService.formatMoney(iss));
        return sb.toString();
    }

    private static String generateBookingReceipt(Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(doc.reservationId).append("\n");
        sb.append("Guest: ").append(doc.guestName);
        sb.append(" (passport ").append(doc.guestPassport).append(", ").append(doc.guestCountry).append(")\n");
        sb.append("Room: ").append(doc.roomNumber).append(" (").append(doc.roomType).append(")\n");
        sb.append("Check-in: ").append(UtilService.formatDateEnglish(doc.checkIn));
        sb.append(" | Check-out: ").append(UtilService.formatDateEnglish(doc.checkOut));
        sb.append(" | ").append(doc.nights).append(doc.nights == 1 ? " night" : " nights").append("\n");
        sb.append("Nights: ").append(UtilService.formatMoneyEnglish(doc.subtotal)).append("\n");

        if (doc.discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Discount: ").append(UtilService.formatMoneyEnglish(doc.discount)).append("\n");
        }

        sb.append("Total: ").append(UtilService.formatMoneyEnglish(doc.total));
        return sb.toString();
    }

    private static String formatCPF(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
               cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private static String formatCNPJ(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) return cnpj;
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." +
               cnpj.substring(5, 8) + "/" + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
    }
}
