package br.com.marealta.pousada.service;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.NotAllowedException;
import br.com.marealta.pousada.domain.Quote;
import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationStatus;
import br.com.marealta.pousada.domain.Room;
import br.com.marealta.pousada.domain.RoomCatalog;
import br.com.marealta.pousada.notification.Message;
import br.com.marealta.pousada.notification.ReservationEvents;
import br.com.marealta.pousada.persistence.Database;
import br.com.marealta.pousada.persistence.MessageRepository;
import br.com.marealta.pousada.persistence.ReservationRepository;

import java.sql.SQLException;
import java.util.List;

public class ReservationService {

    /** Uma mudança de situação: o novo estado da reserva e as mensagens que ela provoca. */
    private record Outcome(Reservation reservation, List<Message> messages) {
    }

    private interface Change {
        Outcome apply(Reservation current, Room room);
    }

    private final Database database;
    private final ReservationRepository reservations;
    private final MessageRepository messages;
    private final ReservationEvents events;

    public ReservationService(Database database, ReservationRepository reservations, MessageRepository messages,
                              ReservationEvents events) {
        this.database = database;
        this.reservations = reservations;
        this.messages = messages;
        this.events = events;
    }

    public List<Room> rooms() {
        return RoomCatalog.all();
    }

    public Reservation create(NewReservation request) {
        Room room = RoomCatalog.find(request.roomNumber()).orElseThrow(
                () -> new ServiceException(404, "ROOM_NOT_FOUND", "Quarto " + request.roomNumber() + " não existe"));
        if (request.guests() > room.capacity()) {
            throw new ServiceException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        Quote quote = Quote.of(room, request.checkIn(), request.checkOut(), request.ratePlan());
        Reservation draft = new Reservation(0, room.number(), request.guestName(), request.guestEmail(),
                request.guestCpf(), request.checkIn(), request.checkOut(), request.guests(), request.ratePlan(),
                quote.nights(), quote.subtotal(), quote.discount(), quote.total(), ReservationStatus.PENDING,
                Money.ZERO, null);
        return transact(c -> {
            reservations.lockRoom(c, room.number());
            if (reservations.isRoomTaken(c, room.number(), request.checkIn(), request.checkOut())) {
                throw new ServiceException(409, "ROOM_UNAVAILABLE",
                        "O quarto " + room.number() + " já está reservado em alguma dessas noites");
            }
            return reservations.insert(c, draft);
        });
    }

    public Reservation get(long id) {
        return transact(c -> reservations.find(c, id).orElseThrow(() -> notFound(id)));
    }

    public Reservation confirm(long id) {
        return change(id, (r, room) -> {
            Reservation confirmed = r.confirm();
            return new Outcome(confirmed, events.confirmed(confirmed, room));
        });
    }

    public Reservation cancel(long id) {
        return change(id, (r, room) -> {
            Reservation cancelled = r.cancel();
            List<Message> toSend = r.status() == ReservationStatus.CONFIRMED
                    ? events.paidReservationCancelled(cancelled, room)
                    : List.of();
            return new Outcome(cancelled, toSend);
        });
    }

    public Reservation checkIn(long id) {
        return change(id, (r, room) -> new Outcome(r.checkInDone(), List.of()));
    }

    public Receipt checkOut(long id) {
        Reservation done = change(id, (r, room) -> new Outcome(r.checkOutDone(), List.of()));
        return Receipt.of(done, RoomCatalog.find(done.roomNumber()).orElseThrow());
    }

    /** Segunda via do comprovante; só existe para reserva paga. */
    public Receipt receipt(long id) {
        Reservation r = get(id);
        if (!r.status().isPaid()) {
            throw notAllowed("Reserva " + id + " está " + r.status() + " e não tem comprovante");
        }
        return Receipt.of(r, RoomCatalog.find(r.roomNumber()).orElseThrow());
    }

    public List<Message> sentMessages() {
        return transact(messages::findAll);
    }

    private Reservation change(long id, Change change) {
        return transact(c -> {
            Reservation current = reservations.findForUpdate(c, id).orElseThrow(() -> notFound(id));
            Room room = RoomCatalog.find(current.roomNumber()).orElseThrow();
            Outcome outcome;
            try {
                outcome = change.apply(current, room);
            } catch (NotAllowedException e) {
                throw notAllowed(e.getMessage());
            }
            reservations.update(c, outcome.reservation());
            messages.saveAll(c, outcome.messages());
            return outcome.reservation();
        });
    }

    private <T> T transact(Database.Work<T> body) {
        try {
            return database.inTransaction(body);
        } catch (SQLException e) {
            throw new IllegalStateException("Erro no banco de dados", e);
        }
    }

    private static ServiceException notFound(long id) {
        return new ServiceException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe");
    }

    private static ServiceException notAllowed(String message) {
        return new ServiceException(409, "OPERATION_NOT_ALLOWED", message);
    }
}
