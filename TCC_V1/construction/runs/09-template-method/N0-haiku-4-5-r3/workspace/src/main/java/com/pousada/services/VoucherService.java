package com.pousada.services;

import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.utils.FormatUtils;

public class VoucherService {
    private final RoomService roomService;

    public VoucherService(RoomService roomService) {
        this.roomService = roomService;
    }

    public String generateVoucherText(Reservation reservation) {
        Room room = roomService.getRoomByNumber(reservation.roomNumber);

        StringBuilder text = new StringBuilder();
        text.append("Comprovante de reserva nº ").append(reservation.id).append("\n");
        text.append("Hóspede: ").append(reservation.guestName).append(" (CPF ").append(FormatUtils.formatCPF(reservation.guestCpf)).append(")\n");
        text.append("Quarto: ").append(room.number).append(" (").append(room.type).append(")\n");
        text.append("Entrada: ").append(FormatUtils.formatDate(reservation.checkIn)).append(" | ");
        text.append("Saída: ").append(FormatUtils.formatDate(reservation.checkOut)).append(" | ");
        text.append(reservation.nights).append(reservation.nights == 1 ? " diária" : " diárias").append("\n");
        text.append("Diárias: ").append(FormatUtils.formatCurrency(reservation.subtotal)).append("\n");

        if (reservation.discount.compareTo(java.math.BigDecimal.ZERO) > 0) {
            text.append("Desconto: ").append(FormatUtils.formatCurrency(reservation.discount)).append("\n");
        }

        text.append("Total: ").append(FormatUtils.formatCurrency(reservation.total)).append("\n");

        return text.toString();
    }

    public String generateConfirmationEmail(Reservation reservation) {
        String voucher = generateVoucherText(reservation);

        StringBuilder email = new StringBuilder();
        email.append("Olá, ").append(reservation.guestName).append("!\n\n");
        email.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        email.append(voucher).append("\n");
        email.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        email.append("Até breve!\n");

        return email.toString();
    }
}
