package com.pousada.service;

import com.pousada.model.Reservation;
import com.pousada.model.RoomType;
import com.pousada.repository.RoomRepository;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {
    private final RoomRepository roomRepository;

    public DocumentService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public String generateComprovante(Reservation reservation) {
        RoomType roomType = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new RuntimeException("ROOM_NOT_FOUND"))
            .getType();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("pt", "BR"));
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);

        StringBuilder sb = new StringBuilder();
        sb.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        sb.append("Hóspede: ").append(reservation.getGuestName()).append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(reservation.getRoomNumber()).append(" (").append(roomType).append(")\n");
        sb.append("Entrada: ").append(reservation.getCheckIn().format(dateFormatter));
        sb.append(" | Saída: ").append(reservation.getCheckOut().format(dateFormatter));
        sb.append(" | ");

        if (reservation.getNights() == 1) {
            sb.append("1 diária");
        } else {
            sb.append(reservation.getNights()).append(" diárias");
        }

        sb.append("\n");
        sb.append("Diárias: R$ ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("Desconto: R$ ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        sb.append("Total: R$ ").append(formatMoney(reservation.getTotal()));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    private String formatMoney(java.math.BigDecimal value) {
        return String.format("%.2f", value).replace(".", ",");
    }
}
