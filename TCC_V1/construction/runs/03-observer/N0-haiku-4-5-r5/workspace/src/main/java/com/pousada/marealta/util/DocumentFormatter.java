package com.pousada.marealta.util;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.sql.SQLException;

public class DocumentFormatter {
    private static final RoomRepository roomRepository = new RoomRepository();

    public static String format(Reservation reservation) {
        try {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber());
            String roomType = room.getType().name();

            StringBuilder doc = new StringBuilder();
            doc.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
            doc.append("Hóspede: ").append(reservation.getGuestName())
                    .append(" (CPF ").append(reservation.getGuestCpf()).append(")\n");
            doc.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(roomType).append(")\n");

            String nightsLabel = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";
            doc.append("Entrada: ").append(DateFormatter.format(reservation.getCheckIn()))
                    .append(" | Saída: ").append(DateFormatter.format(reservation.getCheckOut()))
                    .append(" | ").append(nightsLabel).append("\n");

            doc.append("Diárias: ").append(MoneyFormatter.format(reservation.getSubtotal())).append("\n");

            if (reservation.getDiscount() != null && reservation.getDiscount().signum() > 0) {
                doc.append("Desconto: ").append(MoneyFormatter.format(reservation.getDiscount())).append("\n");
            }

            doc.append("Total: ").append(MoneyFormatter.format(reservation.getTotal())).append("\n");

            return doc.toString().trim();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to format document", e);
        }
    }
}
