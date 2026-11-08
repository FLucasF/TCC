package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.MessageChannel;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.MessageRepository;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class NotificationService {

    private final MessageRepository messageRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale BR_LOCALE = new Locale("pt", "BR");

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(Reservation reservation, String receiptText) {
        String subject = "Reserva confirmada - Pousada Maré Alta";

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.getGuestName(), receiptText);

        Message message = new Message(
            MessageChannel.EMAIL,
            reservation.getGuestEmail(),
            subject,
            body
        );

        messageRepository.save(message);
    }

    public String generateReceipt(Reservation reservation, String roomType) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(BR_LOCALE);

        String nightsText = reservation.getNights() == 1 ? "1 diária" : reservation.getNights() + " diárias";

        StringBuilder receipt = new StringBuilder();
        receipt.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        receipt.append("Hóspede: ").append(reservation.getGuestName())
               .append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");
        receipt.append("Quarto: ").append(reservation.getRoomNumber())
               .append(" (").append(roomType).append(")\n");
        receipt.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMATTER))
               .append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMATTER))
               .append(" | ").append(nightsText).append("\n");
        receipt.append("Diárias: ").append(currencyFormat.format(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            receipt.append("Desconto: ").append(currencyFormat.format(reservation.getDiscount())).append("\n");
        }

        receipt.append("Total: ").append(currencyFormat.format(reservation.getTotal()));

        return receipt.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9, 11));
    }
}
