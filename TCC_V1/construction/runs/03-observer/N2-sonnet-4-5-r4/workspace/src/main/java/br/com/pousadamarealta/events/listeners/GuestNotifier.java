package br.com.pousadamarealta.events.listeners;

import br.com.pousadamarealta.domain.Message;
import br.com.pousadamarealta.domain.Reservation;
import br.com.pousadamarealta.domain.RoomType;
import br.com.pousadamarealta.events.ReservationConfirmedEvent;
import br.com.pousadamarealta.events.ReservationEvent;
import br.com.pousadamarealta.events.ReservationEventListener;
import br.com.pousadamarealta.repository.MessageRepository;
import br.com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;

public class GuestNotifier implements ReservationEventListener {
    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;

    public GuestNotifier(MessageRepository messageRepository, RoomRepository roomRepository) {
        this.messageRepository = messageRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public void onEvent(ReservationEvent event) {
        if (event instanceof ReservationConfirmedEvent confirmed) {
            handleConfirmed(confirmed);
        }
    }

    private void handleConfirmed(ReservationConfirmedEvent event) {
        Reservation reservation = event.getReservation();
        RoomType roomType = roomRepository.findByNumber(reservation.getRoomNumber()).getType();

        BigDecimal subtotal = reservation.calculateSubtotal(roomType.getNightlyRate());
        BigDecimal discount = reservation.calculateDiscount(subtotal);
        BigDecimal total = subtotal.subtract(discount);

        String receipt = reservation.generateReceipt(roomType, subtotal, discount, total);
        String subject = "Reserva confirmada - Pousada Maré Alta";
        String body = buildConfirmationEmail(reservation, receipt);

        Message message = new Message("EMAIL", reservation.getGuestEmail(), subject, body);
        messageRepository.save(message);
    }

    private String buildConfirmationEmail(Reservation reservation, String receipt) {
        return String.format(
            "Olá, %s!\n\n" +
            "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n" +
            "%s\n\n" +
            "A entrada é a partir das 14h, e a saída, até as 12h.\n" +
            "Até breve!",
            reservation.getGuestName(),
            receipt
        );
    }
}
