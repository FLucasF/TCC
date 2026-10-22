package com.pousadamarealta;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {

    public static Document generateDocument(Reservation reservation, RoomType roomType) {
        if (reservation.guestCpf != null) {
            return generateComprovante(reservation, roomType);
        } else if (reservation.companyCnpj != null) {
            return generateFatura(reservation, roomType);
        } else if (reservation.guestPassport != null) {
            return generateBookingReceipt(reservation, roomType);
        } else {
            throw new IllegalArgumentException("Invalid reservation identification");
        }
    }

    private static Document generateComprovante(Reservation reservation, RoomType roomType) {
        String text = formatComprovante(
            reservation.id,
            reservation.guestName,
            reservation.guestCpf,
            reservation.roomNumber,
            roomType.type,
            reservation.checkIn,
            reservation.checkOut,
            reservation.nights,
            reservation.subtotal,
            reservation.discount,
            reservation.total
        );

        return new Document(
            "Comprovante de reserva",
            reservation.id,
            reservation.guestName,
            reservation.guestCpf,
            null,
            null,
            null,
            null,
            reservation.roomNumber,
            roomType.type,
            formatDateBR(reservation.checkIn),
            formatDateBR(reservation.checkOut),
            reservation.nights,
            formatMoney(reservation.subtotal),
            reservation.discount > 0 ? formatMoney(reservation.discount) : null,
            formatMoney(reservation.total),
            text
        );
    }

    private static Document generateFatura(Reservation reservation, RoomType roomType) {
        double iss = PricingService.round(reservation.total * 0.05);

        String text = formatFatura(
            reservation.id,
            reservation.companyName,
            reservation.companyCnpj,
            reservation.guestName,
            reservation.roomNumber,
            roomType.type,
            reservation.checkIn,
            reservation.checkOut,
            reservation.nights,
            reservation.subtotal,
            reservation.discount,
            reservation.total,
            iss
        );

        return new Document(
            "Fatura de hospedagem",
            reservation.id,
            reservation.guestName,
            null,
            reservation.companyCnpj,
            reservation.companyName,
            null,
            null,
            reservation.roomNumber,
            roomType.type,
            formatDateBR(reservation.checkIn),
            formatDateBR(reservation.checkOut),
            reservation.nights,
            formatMoney(reservation.subtotal),
            reservation.discount > 0 ? formatMoney(reservation.discount) : null,
            formatMoney(reservation.total),
            text
        );
    }

    private static Document generateBookingReceipt(Reservation reservation, RoomType roomType) {
        String text = formatBookingReceipt(
            reservation.id,
            reservation.guestName,
            reservation.guestPassport,
            reservation.guestCountry,
            reservation.roomNumber,
            roomType.type,
            reservation.checkIn,
            reservation.checkOut,
            reservation.nights,
            reservation.subtotal,
            reservation.discount,
            reservation.total
        );

        return new Document(
            "Booking receipt",
            reservation.id,
            reservation.guestName,
            null,
            null,
            null,
            reservation.guestPassport,
            reservation.guestCountry,
            reservation.roomNumber,
            roomType.type,
            formatDateEN(reservation.checkIn),
            formatDateEN(reservation.checkOut),
            reservation.nights,
            formatMoneyBRL(reservation.subtotal),
            reservation.discount > 0 ? formatMoneyBRL(reservation.discount) : null,
            formatMoneyBRL(reservation.total),
            text
        );
    }

    private static String formatComprovante(int id, String guestName, String guestCpf,
                                           int roomNumber, String roomType,
                                           LocalDate checkIn, LocalDate checkOut, int nights,
                                           double subtotal, double discount, double total) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(id).append("\n");
        sb.append("Hóspede: ").append(guestName).append(" (CPF ").append(guestCpf).append(")\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(formatDateBR(checkIn)).append(" | Saída: ").append(formatDateBR(checkOut))
          .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(subtotal)).append("\n");
        if (discount > 0) {
            sb.append("Desconto: ").append(formatMoney(discount)).append("\n");
        }
        sb.append("Total: ").append(formatMoney(total));
        return sb.toString();
    }

    private static String formatFatura(int id, String companyName, String companyCnpj,
                                       String guestName, int roomNumber, String roomType,
                                       LocalDate checkIn, LocalDate checkOut, int nights,
                                       double subtotal, double discount, double total, double iss) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(id).append("\n");
        sb.append("Empresa: ").append(companyName).append(" (CNPJ ").append(companyCnpj).append(")\n");
        sb.append("Hóspede: ").append(guestName).append("\n");
        sb.append("Quarto: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(formatDateBR(checkIn)).append(" | Saída: ").append(formatDateBR(checkOut))
          .append(" | ").append(nights).append(nights == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(subtotal)).append("\n");
        if (discount > 0) {
            sb.append("Desconto: ").append(formatMoney(discount)).append("\n");
        }
        sb.append("Total: ").append(formatMoney(total)).append("\n");
        sb.append("ISS retido (5%): ").append(formatMoney(iss));
        return sb.toString();
    }

    private static String formatBookingReceipt(int id, String guestName, String guestPassport,
                                              String guestCountry, int roomNumber, String roomType,
                                              LocalDate checkIn, LocalDate checkOut, int nights,
                                              double subtotal, double discount, double total) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(id).append("\n");
        sb.append("Guest: ").append(guestName).append(" (passport ").append(guestPassport).append(", ").append(guestCountry).append(")\n");
        sb.append("Room: ").append(roomNumber).append(" (").append(roomType).append(")\n");
        sb.append("Check-in: ").append(formatDateEN(checkIn)).append(" | Check-out: ").append(formatDateEN(checkOut))
          .append(" | ").append(nights).append(nights == 1 ? " night" : " nights").append("\n");
        sb.append("Nights: ").append(formatMoneyBRL(subtotal)).append("\n");
        if (discount > 0) {
            sb.append("Discount: ").append(formatMoneyBRL(discount)).append("\n");
        }
        sb.append("Total: ").append(formatMoneyBRL(total));
        return sb.toString();
    }

    private static String formatDateBR(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String formatDateEN(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private static String formatMoney(double value) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("pt", "BR"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "R$ " + nf.format(value);
    }

    private static String formatMoneyBRL(double value) {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "BRL " + nf.format(value);
    }
}
