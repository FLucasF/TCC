package com.pousadamareaalta.service;

import com.pousadamareaalta.database.*;
import com.pousadamareaalta.domain.*;
import com.pousadamareaalta.util.FormatterUtil;
import java.sql.SQLException;
import java.util.*;

public class MessageService {
    public static void sendConfirmationEmail(Reservation reservation, Room room) throws SQLException {
        String receiptText = generateReceiptText(reservation, room);

        String emailBody = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!
            """,
            reservation.guestName(),
            receiptText
        );

        MessageRepository.create(
            "EMAIL",
            reservation.guestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );
    }

    public static String generateReceiptText(Reservation reservation, Room room) {
        int nights = reservation.nights();
        String nightsLabel = nights == 1 ? "1 diária" : nights + " diárias";

        StringBuilder receipt = new StringBuilder();
        receipt.append("Comprovante de reserva nº ").append(reservation.id()).append("\n");
        receipt.append("Hóspede: ").append(reservation.guestName());
        receipt.append(" (CPF ").append(FormatterUtil.formatCpf(reservation.guestCpf())).append(")\n");
        receipt.append("Quarto: ").append(room.number()).append(" (").append(room.type()).append(")\n");
        receipt.append("Entrada: ").append(FormatterUtil.formatDateBR(reservation.checkIn()));
        receipt.append(" | Saída: ").append(FormatterUtil.formatDateBR(reservation.checkOut()));
        receipt.append(" | ").append(nightsLabel).append("\n");
        receipt.append("Diárias: ").append(FormatterUtil.formatCurrency(reservation.subtotal())).append("\n");

        if (reservation.discount() > 0) {
            receipt.append("Desconto: ").append(FormatterUtil.formatCurrency(reservation.discount())).append("\n");
        }

        receipt.append("Total: ").append(FormatterUtil.formatCurrency(reservation.total()));

        return receipt.toString();
    }

    public static List<Message> getAllMessages() throws SQLException {
        return MessageRepository.findAll();
    }
}
