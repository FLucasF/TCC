package com.pousadamarealta.service;

import com.pousadamarealta.domain.Message;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.infrastructure.MessageRepository;

import java.math.BigDecimal;

public class NotificationService {
    private final MessageRepository messageRepository;

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(Reservation reservation) {
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmail(reservation);
        Message message = Message.email(reservation.getGuestEmail(), subject, body);
        messageRepository.save(message);
    }

    private String buildConfirmationEmail(Reservation reservation) {
        String receipt = buildReceipt(reservation);
        return String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.getGuestName(), receipt);
    }

    public String buildReceipt(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d%n", reservation.getId()));
        sb.append(String.format("Hóspede: %s (CPF %s)%n",
            reservation.getGuestName(), formatCpf(reservation.getGuestCpf())));
        sb.append(String.format("Quarto: %s (%s)%n",
            reservation.getRoomNumber(), reservation.getRoomType().name()));
        sb.append(String.format("Entrada: %s | Saída: %s | %s%n",
            formatDate(reservation.getCheckIn()),
            formatDate(reservation.getCheckOut()),
            formatNights(reservation.getNights())));
        sb.append(String.format("Diárias: %s%n", formatMoney(reservation.calculateSubtotal())));

        BigDecimal discount = reservation.calculateDiscount();
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s%n", formatMoney(discount)));
        }

        sb.append(String.format("Total: %s", formatMoney(reservation.calculateTotal())));

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
        return String.format("%02d/%02d/%04d",
            date.getDayOfMonth(),
            date.getMonthValue(),
            date.getYear());
    }

    private String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("R$ %,.2f", amount).replace(",", "X").replace(".", ",").replace("X", ".");
    }
}
