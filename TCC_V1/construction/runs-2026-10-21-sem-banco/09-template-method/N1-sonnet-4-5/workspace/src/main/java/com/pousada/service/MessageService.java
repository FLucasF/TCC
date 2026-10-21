package com.pousada.service;

import com.pousada.domain.Message;
import com.pousada.domain.MessageChannel;
import com.pousada.domain.Reservation;
import com.pousada.repository.MessageRepository;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MessageService {
    private final MessageRepository messageRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(Reservation reservation) {
        String document = buildDocument(reservation);
        String body = buildConfirmationEmailBody(reservation, document);

        long id = messageRepository.getNextId();
        Message message = new Message(
                id,
                MessageChannel.EMAIL,
                reservation.getGuestEmail(),
                "Reserva confirmada - Pousada Maré Alta",
                body
        );

        messageRepository.save(message);
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    private String buildConfirmationEmailBody(Reservation reservation, String document) {
        return String.format(
                "Olá, %s!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n%s\n\nA entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!",
                reservation.getGuestName(),
                document
        );
    }

    public String buildDocument(Reservation reservation) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        sb.append(String.format("Hóspede: %s (CPF %s)\n",
                reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        sb.append(String.format("Quarto: %s (%s)\n",
                reservation.getRoomNumber(),
                reservation.getRoomType().name()));
        sb.append(String.format("Entrada: %s | Saída: %s | %d %s\n",
                reservation.getCheckIn().format(dateFormatter),
                reservation.getCheckOut().format(dateFormatter),
                reservation.getNights(),
                reservation.getNights() == 1 ? "diária" : "diárias"));
        sb.append(String.format("Diárias: %s\n", formatCurrency(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s\n", formatCurrency(reservation.getDiscount())));
        }

        sb.append(String.format("Total: %s", formatCurrency(reservation.getTotal())));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
        }
        return cpf;
    }

    private String formatCurrency(java.math.BigDecimal amount) {
        return currencyFormatter.format(amount);
    }
}
