package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {
    private final RoomRepository roomRepository;
    private final DateTimeFormatter dateFormatter;
    private final DecimalFormat moneyFormatter;

    public DocumentService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
        this.dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "BR"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        this.moneyFormatter = new DecimalFormat("#,##0.00", symbols);
    }

    public String generateReceipt(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow();

        StringBuilder receipt = new StringBuilder();
        receipt.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        receipt.append("Hóspede: ").append(reservation.getGuestName())
               .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        receipt.append("Quarto: ").append(reservation.getRoomNumber())
               .append(" (").append(room.getType().name()).append(")\n");
        receipt.append("Entrada: ").append(formatDate(reservation.getCheckIn()))
               .append(" | Saída: ").append(formatDate(reservation.getCheckOut()))
               .append(" | ").append(formatNights(reservation.getNights())).append("\n");
        receipt.append("Diárias: ").append(formatMoney(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append("Desconto: ").append(formatMoney(reservation.getDiscount())).append("\n");
        }

        receipt.append("Total: ").append(formatMoney(reservation.getTotal()));

        return receipt.toString();
    }

    private String formatDate(LocalDate date) {
        return date.format(dateFormatter);
    }

    private String formatMoney(BigDecimal amount) {
        return "R$ " + moneyFormatter.format(amount);
    }

    private String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." +
                   cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" +
                   cpf.substring(9, 11);
        }
        return cpf;
    }
}
