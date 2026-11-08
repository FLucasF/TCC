package com.marealta.service;

import com.marealta.db.RoomRepository;
import com.marealta.model.Reservation;
import com.marealta.model.Room;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class ReceiptService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter CURRENCY_FORMAT = DateTimeFormatter.ofPattern("##0.00");
    private final RoomRepository roomRepository;

    public ReceiptService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public String generateReceipt(Reservation reservation) {
        Optional<Room> room = roomRepository.findByNumber(reservation.getRoomNumber());
        if (room.isEmpty()) {
            throw new IllegalStateException("Room not found");
        }

        Room r = room.get();
        StringBuilder sb = new StringBuilder();

        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(r.type()).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMAT)).append(" | ");
        sb.append("Saída: ").append(reservation.getCheckOut().format(DATE_FORMAT)).append(" | ");
        sb.append(reservation.getNights()).append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: R$ ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().signum() > 0) {
            sb.append("Desconto: R$ ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: R$ ").append(formatMoney(reservation.getTotal()));

        return sb.toString();
    }

    private String formatMoney(java.math.BigDecimal value) {
        return String.format("%.2f", value).replace('.', ',');
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}
