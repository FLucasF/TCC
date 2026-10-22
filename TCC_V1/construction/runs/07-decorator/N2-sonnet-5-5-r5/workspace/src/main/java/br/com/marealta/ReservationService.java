package br.com.marealta;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.Money;
import br.com.marealta.messaging.Message;
import br.com.marealta.messaging.MessageSink;
import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.ReservationRepository;
import br.com.marealta.persistence.RoomRepository;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.UnaryOperator;

public final class ReservationService {
    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {
    }

    private final Database database;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final MessageSink messages;

    public ReservationService(Database database, RoomRepository rooms, ReservationRepository reservations,
                              MessageSink messages) {
        this.database = database;
        this.rooms = rooms;
        this.reservations = reservations;
        this.messages = messages;
    }

    public List<Room> rooms() {
        return database.inTransaction(rooms::findAll);
    }

    public Reservation create(NewReservation request) {
        return database.inTransaction(c -> {
            Room room = rooms.findForUpdate(c, request.roomNumber()).orElseThrow(
                    () -> new DomainException(ErrorCode.ROOM_NOT_FOUND, "Room not found: " + request.roomNumber()));
            if (request.guests() > room.capacity()) {
                throw new DomainException(ErrorCode.CAPACITY_EXCEEDED,
                        "Room " + room.number() + " holds up to " + room.capacity() + " guests");
            }
            if (reservations.hasOverlap(c, room.number(), request.checkIn(), request.checkOut())) {
                throw new DomainException(ErrorCode.ROOM_UNAVAILABLE,
                        "Room " + room.number() + " is already reserved in some of these nights");
            }
            Quote quote = Quote.of(room, request.checkIn(), request.checkOut(), request.ratePlan());
            return reservations.insert(c, new Reservation(0, room.number(), request.guestName(),
                    request.guestEmail(), request.guestCpf(), request.checkIn(), request.checkOut(),
                    request.guests(), request.ratePlan(), quote.nights(), quote.subtotal(), quote.discount(),
                    quote.total(), ReservationStatus.PENDING, Money.ZERO, null));
        });
    }

    public Reservation get(long id) {
        return database.inTransaction(c -> load(c, id));
    }

    public Reservation confirm(long id) {
        Reservation confirmed = transition(id, Reservation::confirmed);
        Receipt receipt = receiptOf(confirmed);
        messages.deliver(Message.email(confirmed.guestEmail(), "Reserva confirmada - Pousada Maré Alta",
                "Olá, " + confirmed.guestName() + "!\n\n"
                        + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                        + receipt.toText() + "\n\n"
                        + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                        + "Até breve!"));
        return confirmed;
    }

    public Reservation cancel(long id) {
        return transition(id, Reservation::cancelled);
    }

    public Reservation checkIn(long id) {
        return transition(id, Reservation::checkedIn);
    }

    public Receipt checkOut(long id) {
        return receiptOf(transition(id, Reservation::checkedOut));
    }

    /** The second copy of the receipt: only for paid reservations. */
    public Receipt document(long id) {
        Reservation reservation = get(id);
        if (!reservation.status().isPaid()) {
            throw new DomainException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "No receipt for a reservation in status " + reservation.status());
        }
        return receiptOf(reservation);
    }

    private Receipt receiptOf(Reservation reservation) {
        Room room = database.inTransaction(c -> rooms.find(c, reservation.roomNumber()).orElseThrow());
        return Receipt.of(reservation, room);
    }

    /** The sequence shared by every operation: lock, change, save. The change says what is its own. */
    private Reservation transition(long id, UnaryOperator<Reservation> change) {
        return database.inTransaction(c -> {
            Reservation changed = change.apply(loadForUpdate(c, id));
            reservations.updateState(c, changed);
            return changed;
        });
    }

    private Reservation load(Connection c, long id) throws SQLException {
        return reservations.find(c, id).orElseThrow(() -> notFound(id));
    }

    private Reservation loadForUpdate(Connection c, long id) throws SQLException {
        return reservations.findForUpdate(c, id).orElseThrow(() -> notFound(id));
    }

    private static DomainException notFound(long id) {
        return new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reservation not found: " + id);
    }
}
