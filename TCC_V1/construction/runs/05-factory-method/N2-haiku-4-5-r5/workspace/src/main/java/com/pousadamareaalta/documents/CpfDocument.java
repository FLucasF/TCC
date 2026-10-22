package com.pousadamareaalta.documents;

import com.pousadamareaalta.models.Reservation;
import com.pousadamareaalta.models.Room;
import com.pousadamareaalta.utils.PriceCalculator;

public class CpfDocument implements DocumentGenerator {
    @Override
    public String getTitle() {
        return "Comprovante de reserva";
    }

    @Override
    public String generate(Reservation reservation, Room room, long reservationId) {
        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservationId).append("\n");
        sb.append("Hóspede: ").append(reservation.guestName).append(" (CPF ").append(formatCpf(reservation.guestCpf)).append(")\n");
        sb.append("Quarto: ").append(room.number).append(" (").append(room.type).append(")\n");
        sb.append("Entrada: ").append(formatDate(reservation.checkIn));
        sb.append(" | Saída: ").append(formatDate(reservation.checkOut));
        sb.append(" | ").append(getNightsLabel(reservation.nights)).append("\n");
        sb.append("Diárias: R$ ").append(formatMoney(reservation.subtotal)).append("\n");

        if (reservation.discount > 0) {
            sb.append("Desconto: R$ ").append(formatMoney(reservation.discount)).append("\n");
        }

        sb.append("Total: R$ ").append(formatMoney(reservation.total));

        return sb.toString();
    }

    private String getNightsLabel(int nights) {
        if (nights == 1) {
            return "1 diária";
        }
        return nights + " diárias";
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.isEmpty()) return "";
        return cpf.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }

    private String formatDate(java.time.LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(double value) {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("pt", "BR"));
        return nf.format(value);
    }
}
