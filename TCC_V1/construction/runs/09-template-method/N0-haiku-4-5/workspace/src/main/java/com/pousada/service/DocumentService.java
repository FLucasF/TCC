package com.pousada.service;

import com.pousada.model.Reservation;
import com.pousada.repository.RoomRepository;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

public class DocumentService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FORMATTER = new DecimalFormat("0.00");

    static {
        MONEY_FORMATTER.setGroupingUsed(true);
        MONEY_FORMATTER.setGroupingSize(3);
    }

    public static String generateReceipt(Reservation reservation) {
        var room = RoomRepository.findByNumber(reservation.getRoomNumber()).get();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n", reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        sb.append(String.format("Quarto: %d (%s)\n", reservation.getRoomNumber(), room.getType()));

        String checkInFormatted = reservation.getCheckIn().format(DATE_FORMATTER);
        String checkOutFormatted = reservation.getCheckOut().format(DATE_FORMATTER);
        String nightsText = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";
        sb.append(String.format("Entrada: %s | Saída: %s | %s\n", checkInFormatted, checkOutFormatted, nightsText));

        sb.append(String.format("Diárias: R$ %s\n", formatMoney(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: R$ %s\n", formatMoney(reservation.getDiscount())));
        }

        sb.append(String.format("Total: R$ %s", formatMoney(reservation.getTotal())));

        return sb.toString();
    }

    private static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9, 11));
    }

    private static String formatMoney(java.math.BigDecimal value) {
        return String.format("%.2f", value);
    }
}
