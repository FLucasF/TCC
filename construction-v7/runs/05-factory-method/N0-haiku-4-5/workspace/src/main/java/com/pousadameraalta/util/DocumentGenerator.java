package com.pousadameraalta.util;

import com.pousadameraalta.model.Reservation;

public class DocumentGenerator {

    public static String generateReceipt(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append(" (CPF ")
          .append(Formatter.formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(getQuartType(reservation.getRoomNumber())).append(")\n");
        sb.append("Entrada: ").append(Formatter.formatPtDate(reservation.getCheckIn()))
          .append(" | Saída: ").append(Formatter.formatPtDate(reservation.getCheckOut()))
          .append(" | ").append(getNightsText(reservation.getNights(), true)).append("\n");
        sb.append("Diárias: ").append(Formatter.formatBrl(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().signum() > 0) {
            sb.append("Desconto: ").append(Formatter.formatBrl(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(Formatter.formatBrl(reservation.getTotal())).append("\n");

        return sb.toString();
    }

    public static String generateInvoice(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(reservation.getId()).append("\n");
        sb.append("Empresa: ").append(reservation.getCompanyName()).append(" (CNPJ ")
          .append(Formatter.formatCnpj(reservation.getCompanyCnpj())).append(")\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append("\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(getQuartType(reservation.getRoomNumber())).append(")\n");
        sb.append("Entrada: ").append(Formatter.formatPtDate(reservation.getCheckIn()))
          .append(" | Saída: ").append(Formatter.formatPtDate(reservation.getCheckOut()))
          .append(" | ").append(getNightsText(reservation.getNights(), true)).append("\n");
        sb.append("Diárias: ").append(Formatter.formatBrl(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().signum() > 0) {
            sb.append("Desconto: ").append(Formatter.formatBrl(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(Formatter.formatBrl(reservation.getTotal())).append("\n");

        var iss = PriceCalculator.calculateIss(reservation.getTotal());
        sb.append("ISS retido (5%): ").append(Formatter.formatBrl(iss)).append("\n");

        return sb.toString();
    }

    public static String generateBookingReceipt(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Booking receipt no. ").append(reservation.getId()).append("\n");
        sb.append("Guest: ").append(reservation.getGuestName()).append(" (passport ")
          .append(reservation.getGuestPassport()).append(", ").append(reservation.getGuestCountry()).append(")\n");
        sb.append("Room: ").append(reservation.getRoomNumber()).append(" (").append(getQuartType(reservation.getRoomNumber())).append(")\n");
        sb.append("Check-in: ").append(Formatter.formatEnDate(reservation.getCheckIn()))
          .append(" | Check-out: ").append(Formatter.formatEnDate(reservation.getCheckOut()))
          .append(" | ").append(getNightsText(reservation.getNights(), false)).append("\n");
        sb.append("Nights: ").append(Formatter.formatBrlEnglish(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().signum() > 0) {
            sb.append("Discount: ").append(Formatter.formatBrlEnglish(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(Formatter.formatBrlEnglish(reservation.getTotal())).append("\n");

        return sb.toString();
    }

    private static String getQuartType(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };
    }

    private static String getNightsText(int nights, boolean portuguese) {
        if (portuguese) {
            return nights == 1 ? "1 diária" : nights + " diárias";
        } else {
            return nights == 1 ? "1 night" : nights + " nights";
        }
    }
}
