package com.pousadamarealta.service;

import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationDocument;
import com.pousadamarealta.messaging.*;
import com.pousadamarealta.repository.MessageRepository;

import java.util.ArrayList;
import java.util.List;

public final class MessageService {
    private final MessageRepository messageRepository;
    private final MessageDispatcher dispatcher;

    public MessageService(MessageRepository messageRepository, boolean testMode, boolean archiveCopy) {
        this.messageRepository = messageRepository;
        this.dispatcher = createDispatcher(testMode, archiveCopy);
    }

    private MessageDispatcher createDispatcher(boolean testMode, boolean archiveCopy) {
        List<MessageRule> rules = new ArrayList<>();
        rules.add(new EmailValidationRule());

        if (testMode) {
            rules.add(new TestModeRule());
        }

        if (archiveCopy && !testMode) {
            rules.add(new ArchiveCopyRule());
        }

        return new MessageDispatcher(rules);
    }

    public void sendReservationConfirmation(Reservation reservation, ReservationDocument document) {
        String emailBody = buildConfirmationEmail(reservation, document);
        OutgoingMessage message = new OutgoingMessage(
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );

        send(message);
    }

    private String buildConfirmationEmail(Reservation reservation, ReservationDocument document) {
        return String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """,
            reservation.getGuestName(),
            formatDocument(document)
        );
    }

    private String formatDocument(ReservationDocument doc) {
        StringBuilder sb = new StringBuilder();
        sb.append(doc.title()).append(" nº ").append(doc.reservationId()).append("\n");
        sb.append("Hóspede: ").append(doc.guestName()).append(" (CPF ").append(formatCpf(doc.guestCpf())).append(")\n");
        sb.append("Quarto: ").append(doc.roomNumber()).append(" (").append(doc.roomType()).append(")\n");
        sb.append("Entrada: ").append(formatDate(doc.checkIn())).append(" | ");
        sb.append("Saída: ").append(formatDate(doc.checkOut())).append(" | ");
        sb.append(doc.nights()).append(doc.nights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatMoney(doc.subtotal())).append("\n");

        if (doc.discount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatMoney(doc.discount())).append("\n");
        }

        sb.append("Total: ").append(formatMoney(doc.total()));

        return sb.toString();
    }

    private void send(OutgoingMessage message) {
        MessageContext context = dispatcher.dispatch(message);

        messageRepository.save(
            MessageChannel.EMAIL,
            context.getMessage().to(),
            context.getMessage().subject(),
            context.getMessage().body(),
            context.getStatus()
        );

        for (OutgoingMessage additional : context.getAdditionalMessages()) {
            messageRepository.save(
                MessageChannel.EMAIL,
                additional.to(),
                additional.subject(),
                additional.body(),
                MessageStatus.SENT
            );
        }
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatDate(java.time.LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatMoney(java.math.BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}
