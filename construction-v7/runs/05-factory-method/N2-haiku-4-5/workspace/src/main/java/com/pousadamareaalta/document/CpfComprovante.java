package com.pousadamareaalta.document;

import com.pousadamareaalta.model.Document;
import com.pousadamareaalta.model.Reservation;
import com.pousadamareaalta.model.Room;
import com.pousadamareaalta.util.DateUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class CpfComprovante implements DocumentGenerator {
    @Override
    public Document generate(Reservation reservation, Room room) {
        String checkInStr = DateUtils.formatDate(reservation.getCheckIn());
        String checkOutStr = DateUtils.formatDate(reservation.getCheckOut());
        String nightsStr = getNightsLabel(reservation.getNights());
        String subtotalStr = formatMoney(reservation.getSubtotal());
        String discountStr = reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0
            ? formatMoney(reservation.getDiscount())
            : null;
        String totalStr = formatMoney(reservation.getTotal());

        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName())
            .append(" (CPF ").append(reservation.getGuestCpf()).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber())
            .append(" (").append(room.type()).append(")\n");
        sb.append("Entrada: ").append(checkInStr)
            .append(" | Saída: ").append(checkOutStr)
            .append(" | ").append(nightsStr).append("\n");
        sb.append("Diárias: ").append(subtotalStr).append("\n");
        if (discountStr != null) {
            sb.append("Desconto: ").append(discountStr).append("\n");
        }
        sb.append("Total: ").append(totalStr);

        return new Document(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            null,
            null,
            null,
            null,
            reservation.getRoomNumber(),
            room.type().toString(),
            checkInStr,
            checkOutStr,
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal(),
            sb.toString()
        );
    }

    private String getNightsLabel(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private String formatMoney(BigDecimal value) {
        String formatted = String.format("%.2f", value);
        return "R$ " + formatted.replace(".", ",");
    }
}
