package br.com.pousada.service;

import br.com.pousada.model.Reservation;
import br.com.pousada.repository.MessageRepository;
import br.com.pousada.util.DateUtil;
import br.com.pousada.util.MoneyUtil;
import br.com.pousada.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final MessageRepository messageRepository = new MessageRepository();

    public void sendGuestConfirmationEmail(Reservation reservation) {
        String receipt = formatReceipt(reservation);

        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.guestName, receipt);

        Message message = new Message(0, "EMAIL", reservation.guestEmail, subject, body);
        messageRepository.save(message);
        logger.info("E-mail de confirmação enviado para {}", reservation.guestEmail);
    }

    public void sendHousekeepingNotification(Reservation reservation) {
        String subject = String.format("Reserva confirmada - quarto %d", reservation.roomNumber);
        String body = String.format("Entrada: %s | Saída: %s | %d hóspedes",
            DateUtil.formatBrDate(reservation.checkIn),
            DateUtil.formatBrDate(reservation.checkOut),
            reservation.guests);

        Message message = new Message(0, "EMAIL", "governanca@pousadamarealta.com.br", subject, body);
        messageRepository.save(message);
        logger.info("E-mail de governança enviado para reserva {}", reservation.id);
    }

    public void sendFinanceNotificationConfirmation(Reservation reservation) {
        String subject = String.format("Pagamento recebido - reserva %d", reservation.id);
        String body = String.format("Valor pago: %s", MoneyUtil.formatBr(reservation.amountPaid));

        Message message = new Message(0, "EMAIL", "financeiro@pousadamarealta.com.br", subject, body);
        messageRepository.save(message);
        logger.info("E-mail de financeiro enviado para reserva {}", reservation.id);
    }

    public void sendHousekeepingCancellationNotification(Reservation reservation) {
        String subject = String.format("Reserva cancelada - quarto %d", reservation.roomNumber);
        String body = String.format("Entrada: %s | Saída: %s",
            DateUtil.formatBrDate(reservation.checkIn),
            DateUtil.formatBrDate(reservation.checkOut));

        Message message = new Message(0, "EMAIL", "governanca@pousadamarealta.com.br", subject, body);
        messageRepository.save(message);
        logger.info("E-mail de cancelamento enviado para governança");
    }

    public void sendFinanceNotificationCancellation(Reservation reservation, double refundAmount) {
        String subject = String.format("Reembolso - reserva %d", reservation.id);
        String body = String.format("Valor a devolver: %s", MoneyUtil.formatBr(refundAmount));

        Message message = new Message(0, "EMAIL", "financeiro@pousadamarealta.com.br", subject, body);
        messageRepository.save(message);
        logger.info("E-mail de reembolso enviado para financeiro");
    }

    private String formatReceipt(Reservation reservation) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comprovante de reserva nº %d%n", reservation.id));
        sb.append(String.format("Hóspede: %s (CPF %s)%n", reservation.guestName, formatCpf(reservation.guestCpf)));
        sb.append(String.format("Quarto: %d (%s)%n", reservation.roomNumber, getQuartoType(reservation.roomNumber)));

        String nightsLabel = reservation.nights == 1 ? "1 diária" : reservation.nights + " diárias";
        sb.append(String.format("Entrada: %s | Saída: %s | %s%n",
            DateUtil.formatBrDate(reservation.checkIn),
            DateUtil.formatBrDate(reservation.checkOut),
            nightsLabel));

        sb.append(String.format("Diárias: %s%n", MoneyUtil.formatBr(reservation.subtotal)));

        if (reservation.discount > 0) {
            sb.append(String.format("Desconto: %s%n", MoneyUtil.formatBr(reservation.discount)));
        }

        sb.append(String.format("Total: %s", MoneyUtil.formatBr(reservation.total)));

        return sb.toString();
    }

    private String formatCpf(String cpf) {
        if (cpf.length() != 11) return cpf;
        return String.format("%s.%s.%s-%s",
            cpf.substring(0, 3),
            cpf.substring(3, 6),
            cpf.substring(6, 9),
            cpf.substring(9));
    }

    private String getQuartoType(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };
    }
}
