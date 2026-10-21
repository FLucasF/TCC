package com.pousada.service;

import com.pousada.domain.MessageChannel;
import com.pousada.domain.Money;
import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.repository.MessageRepository;
import com.pousada.util.DateFormatter;

public class NotificationService {
    private final MessageRepository messageRepository;
    private final RoomService roomService;
    private final DocumentService documentService;

    public NotificationService(MessageRepository messageRepository, RoomService roomService,
                              DocumentService documentService) {
        this.messageRepository = messageRepository;
        this.roomService = roomService;
        this.documentService = documentService;
    }

    public void sendGuestConfirmationEmail(Reservation reservation, Money total) {
        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        Money subtotal = reservation.calculateSubtotal(room.getNightlyRate());
        Money discount = reservation.calculateDiscount(subtotal);

        String receipt = documentService.generateReceipt(reservation, room, subtotal, discount, total);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format(
            "Olá, %s!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            receipt
        );

        messageRepository.save(MessageChannel.EMAIL, reservation.getGuestEmail(), subject, body);
    }

    public void sendHousekeepingConfirmationEmail(Reservation reservation) {
        String subject = String.format("Reserva confirmada - quarto %s", reservation.getRoomNumber());
        String body = String.format(
            "Nova reserva confirmada:\n\n" +
            "Quarto: %s\n" +
            "Entrada: %s\n" +
            "Saída: %s\n" +
            "Hóspedes: %d",
            reservation.getRoomNumber(),
            DateFormatter.formatBR(reservation.getCheckIn()),
            DateFormatter.formatBR(reservation.getCheckOut()),
            reservation.getGuests()
        );

        messageRepository.save(MessageChannel.EMAIL, "governanca@pousadamarealta.com.br", subject, body);
    }

    public void sendFinancePaymentEmail(Reservation reservation, Money amount) {
        String subject = String.format("Pagamento recebido - reserva %d", reservation.getId());
        String body = String.format(
            "Pagamento recebido:\n\n" +
            "Reserva: %d\n" +
            "Valor: %s",
            reservation.getId(),
            amount.formatBRL()
        );

        messageRepository.save(MessageChannel.EMAIL, "financeiro@pousadamarealta.com.br", subject, body);
    }

    public void sendHousekeepingCancellationEmail(Reservation reservation) {
        String subject = String.format("Reserva cancelada - quarto %s", reservation.getRoomNumber());
        String body = String.format(
            "Reserva cancelada:\n\n" +
            "Quarto: %s\n" +
            "Datas liberadas: %s a %s",
            reservation.getRoomNumber(),
            DateFormatter.formatBR(reservation.getCheckIn()),
            DateFormatter.formatBR(reservation.getCheckOut())
        );

        messageRepository.save(MessageChannel.EMAIL, "governanca@pousadamarealta.com.br", subject, body);
    }

    public void sendFinanceRefundEmail(Reservation reservation, Money refundAmount) {
        String subject = String.format("Reembolso - reserva %d", reservation.getId());
        String body = String.format(
            "Reembolso a processar:\n\n" +
            "Reserva: %d\n" +
            "Valor: %s",
            reservation.getId(),
            refundAmount.formatBRL()
        );

        messageRepository.save(MessageChannel.EMAIL, "financeiro@pousadamarealta.com.br", subject, body);
    }
}
