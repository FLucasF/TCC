package com.pousadamareaalta.documents;

import com.pousadamareaalta.models.Reservation;
import com.pousadamareaalta.models.Room;
import com.pousadamareaalta.utils.PriceCalculator;

public class CnpjDocument implements DocumentGenerator {
    @Override
    public String getTitle() {
        return "Fatura de hospedagem";
    }

    @Override
    public String generate(Reservation reservation, Room room, long reservationId) {
        double issAmount = PriceCalculator.round(reservation.total * 0.05);

        StringBuilder sb = new StringBuilder();
        sb.append("Fatura de hospedagem nº ").append(reservationId).append("\n");
        sb.append("Empresa: ").append(reservation.companyName).append(" (CNPJ ").append(formatCnpj(reservation.companyCnpj)).append(")\n");
        sb.append("Hóspede: ").append(reservation.guestName).append("\n");
        sb.append("Quarto: ").append(room.number).append(" (").append(room.type).append(")\n");
        sb.append("Entrada: ").append(formatDate(reservation.checkIn));
        sb.append(" | Saída: ").append(formatDate(reservation.checkOut));
        sb.append(" | ").append(getNightsLabel(reservation.nights)).append("\n");
        sb.append("Diárias: R$ ").append(formatMoney(reservation.subtotal)).append("\n");

        if (reservation.discount > 0) {
            sb.append("Desconto: R$ ").append(formatMoney(reservation.discount)).append("\n");
        }

        sb.append("Total: R$ ").append(formatMoney(reservation.total)).append("\n");
        sb.append("ISS retido (5%): R$ ").append(formatMoney(issAmount));

        return sb.toString();
    }

    private String getNightsLabel(int nights) {
        if (nights == 1) {
            return "1 diária";
        }
        return nights + " diárias";
    }

    private String formatCnpj(String cnpj) {
        if (cnpj == null || cnpj.isEmpty()) return "";
        return cnpj.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
    }

    private String formatDate(java.time.LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(double value) {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("pt", "BR"));
        return nf.format(value);
    }
}
