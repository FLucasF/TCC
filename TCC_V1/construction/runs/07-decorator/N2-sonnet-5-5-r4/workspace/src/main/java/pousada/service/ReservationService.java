package pousada.service;

import java.util.List;

import pousada.domain.ApiException;
import pousada.domain.Money;
import pousada.domain.Pricing;
import pousada.domain.Receipt;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;
import pousada.domain.Room;
import pousada.messaging.Message;
import pousada.messaging.MessageSink;
import pousada.persistence.ReservationRepository;
import pousada.persistence.RoomRepository;

public final class ReservationService {
    private static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final MessageSink messages;

    public ReservationService(RoomRepository rooms, ReservationRepository reservations, MessageSink messages) {
        this.rooms = rooms;
        this.reservations = reservations;
        this.messages = messages;
    }

    public List<Room> rooms() {
        return rooms.all();
    }

    public Reservation create(NewReservation request) {
        Room room = room(request.roomNumber());
        if (request.guests() > room.capacity()) {
            throw ApiException.capacityExceeded(room.capacity());
        }
        Pricing.Quote quote = Pricing.quote(room, request.checkIn(), request.checkOut(), request.ratePlan());
        Reservation draft = new Reservation(0, room.number(), request.guestName(), request.guestEmail(),
                request.guestCpf(), request.checkIn(), request.checkOut(), request.guests(), request.ratePlan(),
                quote.nights(), quote.subtotal(), quote.discount(), quote.total(), ReservationStatus.PENDING,
                Money.ZERO, null);
        return reservations.insertIfRoomFree(draft).orElseThrow(ApiException::roomUnavailable);
    }

    public Reservation get(String id) {
        return apply(id, r -> r, false);
    }

    public Reservation confirm(String id) {
        Reservation confirmed = apply(id, Reservation::confirmed, true);
        messages.send(Message.email(confirmed.guestEmail(), CONFIRMATION_SUBJECT, confirmationBody(confirmed)));
        return confirmed;
    }

    public Reservation cancel(String id) {
        return apply(id, Reservation::cancelled, true);
    }

    public Reservation checkIn(String id) {
        return apply(id, Reservation::checkedIn, true);
    }

    public Receipt checkOut(String id) {
        return receipt(apply(id, Reservation::checkedOut, true));
    }

    public Receipt document(String id) {
        Reservation reservation = get(id);
        reservation.requirePaid();
        return receipt(reservation);
    }

    private Reservation apply(String id, java.util.function.UnaryOperator<Reservation> change, boolean write) {
        long number;
        try {
            number = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw ApiException.reservationNotFound(id);
        }
        var result = write ? reservations.update(number, change) : reservations.find(number);
        return result.orElseThrow(() -> ApiException.reservationNotFound(id));
    }

    private Receipt receipt(Reservation reservation) {
        return Receipt.of(reservation, room(reservation.roomNumber()));
    }

    private Room room(int number) {
        return rooms.find(number).orElseThrow(() -> ApiException.roomNotFound(number));
    }

    private String confirmationBody(Reservation reservation) {
        return "Olá, " + reservation.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + receipt(reservation).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
    }
}
