package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.ReservationStatus;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final RoomRepository roomRepository;

    public DocumentService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public String generateReceipt(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.PENDING ||
            reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Receipt can only be generated for paid reservations");
        }

        String roomType = roomRepository.findByNumber(reservation.getRoomNumber())
                .map(r -> r.getType().name())
                .orElse("");

        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d%n", reservation.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)%n",
                reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        receipt.append(String.format("Quarto: %s (%s)%n", reservation.getRoomNumber(), roomType));
        receipt.append(String.format("Entrada: %s | Saída: %s | %s%n",
                reservation.getCheckIn().format(DATE_FORMATTER),
                reservation.getCheckOut().format(DATE_FORMATTER),
                formatNights(reservation.getNights())));
        receipt.append(String.format("Diárias: %s%n", formatCurrency(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: %s%n", formatCurrency(reservation.getDiscount())));
        }

        receipt.append(String.format("Total: %s", formatCurrency(reservation.getTotal())));

        return receipt.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9, 11));
    }

    private String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private String formatCurrency(BigDecimal value) {
        return CURRENCY_FORMAT.format(value);
    }
}
