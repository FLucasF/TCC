package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.RoomRepository;

import java.time.format.DateTimeFormatter;

public class ReceiptService {
    private final RoomRepository roomRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ReceiptService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public String generateReceipt(Reservation reservation) {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber());

        StringBuilder receipt = new StringBuilder();
        receipt.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        receipt.append("Hóspede: ").append(reservation.getGuestName())
               .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        receipt.append("Quarto: ").append(room.getNumber()).append(" (").append(room.getType().getName()).append(")\n");
        receipt.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMATTER))
               .append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMATTER))
               .append(" | ").append(reservation.getNights()).append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        receipt.append("Diárias: ").append(formatCurrency(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().signum() > 0) {
            receipt.append("Desconto: ").append(formatCurrency(reservation.getDiscount())).append("\n");
        }

        receipt.append("Total: ").append(formatCurrency(reservation.getTotal()));

        return receipt.toString();
    }

    public String generateConfirmationEmail(Reservation reservation) {
        String receipt = generateReceipt(reservation);

        StringBuilder email = new StringBuilder();
        email.append("Olá, ").append(reservation.getGuestName()).append("!\n\n");
        email.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        email.append(receipt).append("\n\n");
        email.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        email.append("Até breve!");

        return email.toString();
    }

    private String formatCpf(String cpf) {
        return cpf.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
    }

    private String formatCurrency(java.math.BigDecimal value) {
        return String.format("R$ %,.2f", value).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}
