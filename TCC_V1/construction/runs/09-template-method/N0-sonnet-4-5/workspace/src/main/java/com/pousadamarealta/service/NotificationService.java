package com.pousadamarealta.service;

import com.pousadamarealta.dto.ReceiptDto;
import com.pousadamarealta.model.Message;
import com.pousadamarealta.repository.MessageRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class NotificationService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private final MessageRepository messageRepository;

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(String guestName, String guestEmail, ReceiptDto receipt) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmail(guestName, receipt);
        Message message = new Message("EMAIL", guestEmail, subject, body);
        messageRepository.save(message);
    }

    private String buildConfirmationEmail(String guestName, ReceiptDto receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append("Olá, ").append(guestName).append("!\n\n");
        sb.append("Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n");
        sb.append(formatReceipt(receipt));
        sb.append("\n");
        sb.append("A entrada é a partir das 14h, e a saída, até as 12h.\n");
        sb.append("Até breve!");
        return sb.toString();
    }

    public String formatReceipt(ReceiptDto receipt) {
        StringBuilder sb = new StringBuilder();
        sb.append(receipt.title()).append(" nº ").append(receipt.reservationId()).append("\n");
        sb.append("Hóspede: ").append(receipt.guestName()).append(" (CPF ").append(formatCpf(receipt.guestCpf())).append(")\n");
        sb.append("Quarto: ").append(receipt.roomNumber()).append(" (").append(receipt.roomType()).append(")\n");
        sb.append("Entrada: ").append(formatDate(receipt.checkIn()));
        sb.append(" | Saída: ").append(formatDate(receipt.checkOut()));
        sb.append(" | ").append(receipt.nights()).append(receipt.nights() == 1 ? " diária" : " diárias").append("\n");
        sb.append("Diárias: ").append(formatCurrency(receipt.subtotal())).append("\n");

        if (receipt.discount() != null && receipt.discount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("Desconto: ").append(formatCurrency(receipt.discount())).append("\n");
        }

        sb.append("Total: ").append(formatCurrency(receipt.total()));
        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    private String formatCurrency(BigDecimal amount) {
        return CURRENCY_FORMATTER.format(amount);
    }
}
