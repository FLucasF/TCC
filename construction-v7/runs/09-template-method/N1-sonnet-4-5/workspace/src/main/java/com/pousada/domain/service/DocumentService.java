package com.pousada.domain.service;

import com.pousada.domain.model.MessageChannel;
import com.pousada.domain.model.Reservation;
import com.pousada.domain.model.Room;
import com.pousada.domain.validation.ReservationValidator;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DocumentService {
    private static final Locale BRAZIL = new Locale("pt", "BR");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(BRAZIL);

    private final RoomService roomService;
    private final MessageService messageService;

    public DocumentService(RoomService roomService, MessageService messageService) {
        this.roomService = roomService;
        this.messageService = messageService;
    }

    public String generateDocument(Reservation reservation) {
        ReservationValidator.validateDocumentGeneration(reservation);

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());

        StringBuilder doc = new StringBuilder();
        doc.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        doc.append("Hóspede: ").append(reservation.getGuest().getName())
           .append(" (CPF ").append(formatCpf(reservation.getGuest().getCpf())).append(")\n");
        doc.append("Quarto: ").append(reservation.getRoomNumber())
           .append(" (").append(room.getType()).append(")\n");
        doc.append("Entrada: ").append(formatDate(reservation.getCheckIn()))
           .append(" | Saída: ").append(formatDate(reservation.getCheckOut()))
           .append(" | ").append(formatNights(reservation.getNights())).append("\n");
        doc.append("Diárias: ").append(formatCurrency(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            doc.append("Desconto: ").append(formatCurrency(reservation.getDiscount())).append("\n");
        }

        doc.append("Total: ").append(formatCurrency(reservation.getTotal()));

        return doc.toString();
    }

    public void sendConfirmationEmail(Reservation reservation) {
        String document = generateDocument(reservation);
        String body = buildEmailBody(reservation.getGuest().getName(), document);

        messageService.sendMessage(
            MessageChannel.EMAIL,
            reservation.getGuest().getEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );
    }

    private String buildEmailBody(String guestName, String document) {
        return "Olá, " + guestName + "!\n\n" +
               "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
               document + "\n\n" +
               "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
               "Até breve!";
    }

    private String formatDate(java.time.LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    private String formatCurrency(BigDecimal amount) {
        return CURRENCY_FORMATTER.format(amount);
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
        }
        return cpf;
    }

    private String formatNights(int nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }
}
