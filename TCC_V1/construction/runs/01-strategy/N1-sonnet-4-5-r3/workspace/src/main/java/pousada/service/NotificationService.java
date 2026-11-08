package pousada.service;

import pousada.domain.Message;
import pousada.domain.Reservation;
import pousada.repository.MessageRepository;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class NotificationService {

    private final MessageRepository messageRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public NotificationService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public void sendConfirmationEmail(Reservation reservation) {
        String receipt = generateReceipt(reservation);
        String emailBody = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.guestName(), receipt);

        Message message = new Message(
            null,
            "EMAIL",
            reservation.guestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            emailBody
        );

        messageRepository.save(message);
    }

    public String generateReceipt(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d%n", reservation.id()));
        sb.append(String.format("Hóspede: %s (CPF %s)%n", reservation.guestName(), formatCpf(reservation.guestCpf())));
        sb.append(String.format("Quarto: %s (%s)%n", reservation.roomNumber(), reservation.roomType()));
        sb.append(String.format("Entrada: %s | Saída: %s | %s%n",
            reservation.checkIn().format(DATE_FORMATTER),
            reservation.checkOut().format(DATE_FORMATTER),
            formatNights(reservation.nights())));
        sb.append(String.format("Diárias: %s%n", formatCurrency(reservation.subtotal())));

        if (reservation.discount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            sb.append(String.format("Desconto: %s%n", formatCurrency(reservation.discount())));
        }

        sb.append(String.format("Total: %s", formatCurrency(reservation.total())));

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

    private String formatCurrency(java.math.BigDecimal value) {
        return CURRENCY_FORMAT.format(value);
    }

    private String formatNights(long nights) {
        return nights == 1 ? "1 diária" : nights + " diárias";
    }
}
