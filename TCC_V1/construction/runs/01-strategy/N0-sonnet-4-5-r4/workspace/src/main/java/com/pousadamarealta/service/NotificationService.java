package com.pousadamarealta.service;

import com.pousadamarealta.model.Message;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.repository.MessageRepository;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class NotificationService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private final MessageRepository messageRepository;

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(Reservation reservation, String roomType) {
        String receipt = formatReceipt(reservation, roomType);

        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.getGuestName(), receipt);

        Message message = new Message(
            "EMAIL",
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );

        messageRepository.save(message);
    }

    public String formatReceipt(Reservation reservation, String roomType) {
        StringBuilder receipt = new StringBuilder();

        receipt.append(String.format("Comprovante de reserva nº %d\n", reservation.getId()));
        receipt.append(String.format("Hóspede: %s (CPF %s)\n",
            reservation.getGuestName(),
            formatCpf(reservation.getGuestCpf())));
        receipt.append(String.format("Quarto: %s (%s)\n",
            reservation.getRoomNumber(),
            roomType));
        receipt.append(String.format("Entrada: %s | Saída: %s | %d diária%s\n",
            reservation.getCheckIn().format(DATE_FORMAT),
            reservation.getCheckOut().format(DATE_FORMAT),
            reservation.getNights(),
            reservation.getNights() == 1 ? "" : "s"));
        receipt.append(String.format("Diárias: %s\n",
            CURRENCY_FORMAT.format(reservation.getSubtotal())));

        if (reservation.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: %s\n",
                CURRENCY_FORMAT.format(reservation.getDiscount())));
        }

        receipt.append(String.format("Total: %s",
            CURRENCY_FORMAT.format(reservation.getTotal())));

        return receipt.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." +
                   cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" +
                   cpf.substring(9);
        }
        return cpf;
    }
}
