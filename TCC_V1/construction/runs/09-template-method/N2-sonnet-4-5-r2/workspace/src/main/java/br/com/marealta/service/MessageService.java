package br.com.marealta.service;

import br.com.marealta.domain.Document;
import br.com.marealta.domain.Message;
import br.com.marealta.repository.MessageRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MessageService {
    private final MessageRepository messageRepository;
    private final NumberFormat currencyFormat;
    private final DateTimeFormatter dateFormatter;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        this.dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    }

    public void sendConfirmationEmail(String email, String guestName, Document document) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmail(guestName, document);
        Message message = new Message(null, "EMAIL", email, subject, body);
        messageRepository.save(message);
    }

    private String buildConfirmationEmail(String guestName, Document document) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(guestName).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(formatDocument(document));
        sb.append("\nA entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    public String formatDocument(Document document) {
        StringBuilder sb = new StringBuilder();
        sb.append(document.getTitle()).append(" nº ").append(document.getReservationId()).append("\n");
        sb.append("Hóspede: ").append(document.getGuestName())
                .append(" (CPF ").append(formatCpf(document.getGuestCpf())).append(")\n");
        sb.append("Quarto: ").append(document.getRoomNumber())
                .append(" (").append(document.getRoomType().name()).append(")\n");
        sb.append("Entrada: ").append(formatDate(document.getCheckIn()))
                .append(" | Saída: ").append(formatDate(document.getCheckOut()))
                .append(" | ").append(document.getNights()).append(document.getNights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatCurrency(document.getSubtotal())).append("\n");
        if (document.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(document.getDiscount())).append("\n");
        }
        sb.append("Total: ").append(formatCurrency(document.getTotal()));
        return sb.toString();
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }

    private String formatCurrency(BigDecimal amount) {
        return currencyFormat.format(amount);
    }

    private String formatDate(LocalDate date) {
        return date.format(dateFormatter);
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                    cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }
}
