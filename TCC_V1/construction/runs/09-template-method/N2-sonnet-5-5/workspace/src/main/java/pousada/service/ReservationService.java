package pousada.service;

import java.time.LocalDate;
import java.util.function.Consumer;
import pousada.db.Database;
import pousada.db.ReservationRepository;
import pousada.domain.AppException;
import pousada.domain.ErrorCode;
import pousada.domain.RatePlan;
import pousada.domain.Receipt;
import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.domain.RoomCatalog;

public class ReservationService {
    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    private final Database db;
    private final RoomCatalog rooms;
    private final ReservationRepository reservations;
    private final ConfirmationNotifier notifier;

    public ReservationService(Database db, RoomCatalog rooms, ReservationRepository reservations,
                              ConfirmationNotifier notifier) {
        this.db = db;
        this.rooms = rooms;
        this.reservations = reservations;
        this.notifier = notifier;
    }

    public synchronized Reservation create(NewReservation n) {
        Room room = rooms.get(n.roomNumber());
        if (n.guests() > room.capacity()) {
            throw new AppException(ErrorCode.CAPACITY_EXCEEDED,
                    "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
        }
        return db.transaction(() -> {
            if (reservations.hasActiveOverlap(room.number(), n.checkIn(), n.checkOut())) {
                throw new AppException(ErrorCode.ROOM_UNAVAILABLE, "O quarto já está reservado em alguma das noites");
            }
            Reservation r = Reservation.create(room, n.guestName(), n.guestEmail(), n.guestCpf(),
                    n.checkIn(), n.checkOut(), n.guests(), n.ratePlan());
            reservations.insert(r);
            return r;
        });
    }

    public Reservation get(String rawId) {
        long id;
        try {
            id = Long.parseLong(rawId);
        } catch (NumberFormatException e) {
            throw notFound(rawId);
        }
        return reservations.find(id).orElseThrow(() -> notFound(rawId));
    }

    public Reservation confirm(String id) {
        return change(id, Reservation::confirm, r -> notifier.confirmed(r, receiptOf(r)));
    }

    public Reservation cancel(String id) {
        return change(id, Reservation::cancel, r -> {});
    }

    public Reservation checkIn(String id) {
        return change(id, Reservation::checkIn, r -> {});
    }

    public Receipt checkOut(String id) {
        return receiptOf(change(id, Reservation::checkOut, r -> {}));
    }

    /** Segunda via: só existe para reserva paga. */
    public Receipt document(String id) {
        Reservation r = get(id);
        if (!r.paid()) {
            throw new AppException(ErrorCode.OPERATION_NOT_ALLOWED, "A reserva não está paga");
        }
        return receiptOf(r);
    }

    private Reservation change(String id, Consumer<Reservation> transition, Consumer<Reservation> afterSave) {
        return db.transaction(() -> {
            Reservation r = get(id);
            transition.accept(r);
            reservations.update(r);
            afterSave.accept(r);
            return r;
        });
    }

    private Receipt receiptOf(Reservation r) {
        return Receipt.of(r, rooms.get(r.getRoomNumber()));
    }

    private static AppException notFound(String id) {
        return new AppException(ErrorCode.RESERVATION_NOT_FOUND, "Reserva " + id + " não existe");
    }
}
