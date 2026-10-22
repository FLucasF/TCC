package com.pousada.services;

import com.pousada.models.Receipt;
import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.repositories.RoomRepository;
import com.pousada.utils.CpfFormatter;
import com.pousada.utils.FormatterUtil;
import java.math.BigDecimal;

public class ReceiptService {
    private final RoomRepository roomRepository;

    public ReceiptService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Receipt generateReceipt(Reservation reservation) throws Exception {
        Room room = roomRepository.findByNumber(reservation.getRoomNumber());

        return new Receipt(
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

    public String formatReceipt(Receipt receipt) {
        StringBuilder sb = new StringBuilder();

        sb.append("Comprovante de reserva nº ").append(receipt.getReservationId()).append("\n");
        sb.append("Hóspede: ").append(receipt.getGuestName())
          .append(" (CPF ").append(CpfFormatter.format(receipt.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(receipt.getRoomNumber()).append(" (").append(receipt.getRoomType()).append(")\n");
        sb.append("Entrada: ").append(FormatterUtil.formatDatePT(receipt.getCheckIn()))
          .append(" | Saída: ").append(FormatterUtil.formatDatePT(receipt.getCheckOut()))
          .append(" | ").append(FormatterUtil.formatNights(receipt.getNights())).append("\n");
        sb.append("Diárias: ").append(FormatterUtil.formatCurrency(receipt.getSubtotal())).append("\n");

        if (receipt.getDiscount() != null && receipt.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(FormatterUtil.formatCurrency(receipt.getDiscount())).append("\n");
        }

        sb.append("Total: ").append(FormatterUtil.formatCurrency(receipt.getTotal()));

        return sb.toString();
    }
}
