package com.pousada.service;

import com.pousada.domain.Document;
import com.pousada.domain.Reservation;
import com.pousada.repository.RoomRepository;

public class DocumentGenerator {
    private final RoomRepository roomRepository;

    public DocumentGenerator(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Document generateDocument(Reservation reservation) {
        var room = roomRepository.findByNumber(reservation.getRoomNumber())
            .orElseThrow(() -> new RuntimeException("Room not found"));

        return new Document(
            "Comprovante de reserva",
            reservation.getId(),
            reservation.getGuestName(),
            reservation.getGuestCpf(),
            reservation.getRoomNumber(),
            room.getType(),
            reservation.getCheckIn(),
            reservation.getCheckOut(),
            reservation.getNights(),
            reservation.getSubtotal(),
            reservation.getDiscount(),
            reservation.getTotal()
        );
    }

    public String formatDocument(Document document) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s nº %d%n", document.getTitle(), document.getReservationId()));
        sb.append(String.format("Hóspede: %s (CPF %s)%n", document.getGuestName(), formatCpf(document.getGuestCpf())));
        sb.append(String.format("Quarto: %d (%s)%n", document.getRoomNumber(), document.getRoomType()));
        sb.append(String.format("Entrada: %s | Saída: %s | %d %s%n",
            formatDate(document.getCheckIn()),
            formatDate(document.getCheckOut()),
            document.getNights(),
            document.getNights() == 1 ? "diária" : "diárias"));
        sb.append(String.format("Diárias: %s%n", formatMoney(document.getSubtotal())));

        if (document.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s%n", formatMoney(document.getDiscount())));
        }

        sb.append(String.format("Total: %s", formatMoney(document.getTotal())));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return String.format("%s.%s.%s-%s",
                cpf.substring(0, 3),
                cpf.substring(3, 6),
                cpf.substring(6, 9),
                cpf.substring(9, 11));
        }
        return cpf;
    }

    private String formatDate(java.time.LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(java.math.BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(',', 'X').replace('.', ',').replace('X', '.');
    }
}
