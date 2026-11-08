package br.com.pousadamarealta.listeners;

import br.com.pousadamarealta.domain.*;
import br.com.pousadamarealta.domain.events.ReservationCancelled;
import br.com.pousadamarealta.domain.events.ReservationConfirmed;
import br.com.pousadamarealta.domain.events.ReservationEventListener;
import br.com.pousadamarealta.repository.MessageRepository;

public final class GuestNotifier implements ReservationEventListener {
    private final MessageRepository messageRepository;

    public GuestNotifier(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void onReservationConfirmed(ReservationConfirmed event) {
        Reservation reservation = event.reservation();
        String receipt = buildReceipt(reservation);
        String body = String.format("""
            Olá, %s!

            Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

            %s

            A entrada é a partir das 14h, e a saída, até as 12h.
            Até breve!""", reservation.guestName(), receipt);

        Message message = new Message(
            MessageChannel.EMAIL,
            reservation.guestEmail(),
            "Reserva confirmada - Pousada Maré Alta",
            body
        );
        messageRepository.save(message);
    }

    @Override
    public void onReservationCancelled(ReservationCancelled event) {
    }

    private String buildReceipt(Reservation reservation) {
        StringBuilder receipt = new StringBuilder();
        receipt.append(String.format("Comprovante de reserva nº %d%n", reservation.id()));
        receipt.append(String.format("Hóspede: %s (CPF %s)%n",
            reservation.guestName(),
            Formatter.formatCpf(reservation.guestCpf())));
        receipt.append(String.format("Quarto: %s (%s)%n",
            reservation.roomNumber(),
            reservation.roomType()));
        receipt.append(String.format("Entrada: %s | Saída: %s | %d %s%n",
            Formatter.formatDate(reservation.checkIn()),
            Formatter.formatDate(reservation.checkOut()),
            reservation.nights(),
            reservation.nights() == 1 ? "diária" : "diárias"));
        receipt.append(String.format("Diárias: %s%n",
            Formatter.formatMoney(reservation.subtotal())));

        if (reservation.discount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Desconto: %s%n",
                Formatter.formatMoney(reservation.discount())));
        }

        receipt.append(String.format("Total: %s",
            Formatter.formatMoney(reservation.total())));

        return receipt.toString();
    }
}
