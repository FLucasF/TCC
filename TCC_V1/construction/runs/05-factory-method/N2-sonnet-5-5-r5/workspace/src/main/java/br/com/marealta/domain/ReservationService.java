package br.com.marealta.domain;

import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.MessageRepository;
import br.com.marealta.persistence.ReservationRepository;

import java.util.List;
import java.util.function.Consumer;

public class ReservationService {
    private final Database db;
    private final ReservationRepository reservations;
    private final MessageRepository messages;

    public ReservationService(Database db, ReservationRepository reservations, MessageRepository messages) {
        this.db = db;
        this.reservations = reservations;
        this.messages = messages;
    }

    public synchronized Reservation create(NewReservation request) {
        Room room = RoomCatalog.find(request.roomNumber())
                .orElseThrow(() -> new DomainException(ErrorCode.ROOM_NOT_FOUND, "Quarto inexistente"));
        if (request.guests() > room.capacity()) {
            throw new DomainException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto comporta no máximo " + room.capacity() + " hóspedes");
        }
        return db.inTransaction(() -> {
            if (reservations.hasOverlap(room.number(), request.checkIn(), request.checkOut())) {
                throw new DomainException(ErrorCode.ROOM_UNAVAILABLE, "Quarto já reservado em alguma das noites");
            }
            return reservations.insert(Reservation.book(room, request.guestName(), request.guestEmail(),
                    request.identity(), request.checkIn(), request.checkOut(), request.guests(),
                    request.ratePlan()));
        });
    }

    public Reservation get(long id) {
        return reservations.find(id).orElseThrow(
                () -> new DomainException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva inexistente"));
    }

    public synchronized Reservation confirm(long id) {
        return change(id, Reservation::confirm, r -> messages.add(ConfirmationEmail.compose(r)));
    }

    public synchronized Reservation cancel(long id) {
        return change(id, Reservation::cancel, r -> { });
    }

    public synchronized Reservation checkIn(long id) {
        return change(id, Reservation::registerCheckIn, r -> { });
    }

    public synchronized ReservationDocument checkOut(long id) {
        return change(id, Reservation::registerCheckOut, r -> { }).document();
    }

    public ReservationDocument document(long id) {
        return get(id).document();
    }

    public List<Message> sentMessages() {
        return messages.findAll();
    }

    /** A mudança de situação e o que vem junto dela valem juntos ou não valem. */
    private Reservation change(long id, Consumer<Reservation> operation, Consumer<Reservation> sideEffect) {
        return db.inTransaction(() -> {
            Reservation r = get(id);
            operation.accept(r);
            reservations.save(r);
            sideEffect.accept(r);
            return r;
        });
    }
}
