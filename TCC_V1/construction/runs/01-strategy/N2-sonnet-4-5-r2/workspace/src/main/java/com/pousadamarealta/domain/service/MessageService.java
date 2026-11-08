package com.pousadamarealta.domain.service;

import com.pousadamarealta.domain.model.Message;
import com.pousadamarealta.domain.model.MessageChannel;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.Room;
import com.pousadamarealta.infrastructure.database.MessageRepository;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MessageService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private final MessageRepository messageRepository;
    private final RoomService roomService;

    public MessageService(MessageRepository messageRepository, RoomService roomService) {
        this.messageRepository = messageRepository;
        this.roomService = roomService;
    }

    public void sendConfirmationEmail(Reservation reservation) {
        String document = generateDocument(reservation);
        String body = buildConfirmationEmailBody(reservation, document);

        Message message = new Message(
            MessageChannel.EMAIL,
            reservation.getGuestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );

        messageRepository.save(message);
    }

    public String generateDocument(Reservation reservation) {
        StringBuilder doc = new StringBuilder();
        doc.append("Comprovante de reserva nº ").append(reservation.getId()).append("\n");
        doc.append("Hóspede: ").append(reservation.getGuestName());
        doc.append(" (CPF ").append(formatCpf(reservation.getGuestCpf())).append(")\n");

        Room room = roomService.getRoomByNumber(reservation.getRoomNumber());
        doc.append("Quarto: ").append(reservation.getRoomNumber());
        doc.append(" (").append(room != null ? room.getType() : "").append(")\n");

        doc.append("Entrada: ").append(reservation.getCheckIn().format(DATE_FORMATTER));
        doc.append(" | Saída: ").append(reservation.getCheckOut().format(DATE_FORMATTER));
        doc.append(" | ").append(reservation.getNights()).append(reservation.getNights() == 1 ? " diária" : " diárias").append("\n");

        doc.append("Diárias: ").append(CURRENCY_FORMATTER.format(reservation.getSubtotal())).append("\n");

        if (reservation.getDiscount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            doc.append("Desconto: ").append(CURRENCY_FORMATTER.format(reservation.getDiscount())).append("\n");
        }

        doc.append("Total: ").append(CURRENCY_FORMATTER.format(reservation.getTotal()));

        return doc.toString();
    }

    private String buildConfirmationEmailBody(Reservation reservation, String document) {
        return String.format(
            "Olá, %s!\n\nSua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!",
            reservation.getGuestName(),
            document
        );
    }

    private String formatCpf(String cpf) {
        if (cpf.length() == 11) {
            return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." +
                   cpf.substring(6, 9) + "-" + cpf.substring(9);
        }
        return cpf;
    }

    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
}
