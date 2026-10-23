package com.pousada.service;

import com.pousada.domain.Message;
import com.pousada.domain.MessageChannel;
import com.pousada.domain.MessageStatus;
import com.pousada.domain.Reservation;
import com.pousada.messaging.MessageProcessor;
import com.pousada.repository.MessageRepository;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MessageService {
    private final MessageRepository messageRepository;
    private final MessageProcessor messageProcessor;
    private static final Locale BRAZIL = Locale.of("pt", "BR");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(BRAZIL);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public MessageService(MessageRepository messageRepository, MessageProcessor messageProcessor) {
        this.messageRepository = messageRepository;
        this.messageProcessor = messageProcessor;
    }

    public void sendConfirmationEmail(Reservation reservation, String document) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """, reservation.getGuestName(), document);

        Message message = new Message(
            MessageChannel.EMAIL,
            reservation.getGuestEmail(),
            subject,
            body,
            MessageStatus.SENT
        );

        List<Message> processedMessages = messageProcessor.process(message);
        for (Message msg : processedMessages) {
            messageRepository.save(msg);
        }
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    public String formatDocument(Reservation reservation, String roomType) {
        StringBuilder doc = new StringBuilder();
        doc.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        doc.append("Hóspede: ").append(reservation.getGuestName())
           .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        doc.append("Quarto: ").append(reservation.getRoomNumber())
           .append(" (").append(roomType).append(")\n");
        doc.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMAT))
           .append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMAT))
           .append(" | ").append(reservation.getNights())
           .append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");
        doc.append("Diárias: ").append(formatCurrency(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            doc.append("Desconto: ").append(formatCurrency(reservation.getDiscount())).append("\n");
        }

        doc.append("Total: ").append(formatCurrency(reservation.getTotal()));

        return doc.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
        }
        return cpf;
    }

    private String formatCurrency(java.math.BigDecimal amount) {
        return CURRENCY_FORMAT.format(amount);
    }
}
