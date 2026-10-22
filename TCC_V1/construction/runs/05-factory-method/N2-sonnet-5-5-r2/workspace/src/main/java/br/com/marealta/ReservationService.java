package br.com.marealta;

import br.com.marealta.domain.ConfirmationEmail;
import br.com.marealta.domain.Document;
import br.com.marealta.domain.Message;
import br.com.marealta.domain.OperationNotAllowedException;
import br.com.marealta.domain.Reports;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.Rooms;
import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.MessageRepository;
import br.com.marealta.persistence.ReservationRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Consumer;

public final class ReservationService {
    private final Database db;
    private final ReservationRepository reservations = new ReservationRepository();
    private final MessageRepository messages = new MessageRepository();
    /** Garante que duas reservas não disputem a mesma noite. */
    private final Object writeLock = new Object();

    public ReservationService(Database db) {
        this.db = db;
    }

    public Reservation create(NewReservation req) {
        synchronized (writeLock) {
            return db.transaction(c -> {
                Room room = Rooms.find(req.roomNumber()).orElseThrow(
                        () -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto não encontrado"));
                if (req.guests() > room.type().capacity()) {
                    throw new ApiException(422, "CAPACITY_EXCEEDED",
                            "O quarto comporta no máximo " + room.type().capacity() + " hóspedes");
                }
                if (reservations.isRoomTaken(c, room.number(), req.checkIn(), req.checkOut())) {
                    throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado no período");
                }
                Reservation r = Reservation.create(room, req.guestName(), req.guestEmail(), req.identity(),
                        req.checkIn(), req.checkOut(), req.guests(), req.ratePlan());
                reservations.insert(c, r);
                return r;
            });
        }
    }

    public Reservation get(long id) {
        return db.transaction(c -> reservations.find(c, id).orElseThrow(ApiException::reservationNotFound));
    }

    public Reservation confirm(long id) {
        return change(id, Reservation::confirm, (c, r) -> {
            messages.insert(c, ConfirmationEmail.of(r));
        });
    }

    public Reservation cancel(long id) {
        return change(id, Reservation::cancel, null);
    }

    public Reservation checkIn(long id) {
        return change(id, Reservation::checkIn, null);
    }

    public Document checkOut(long id) {
        return change(id, Reservation::checkOut, null).document();
    }

    public Document document(long id) {
        try {
            return get(id).document();
        } catch (OperationNotAllowedException e) {
            throw notAllowed(e);
        }
    }

    public List<Message> messages() {
        return db.transaction(messages::all);
    }

    public String occupancyReport(LocalDate night) {
        return Reports.occupancy(db.transaction(c -> reservations.paidOccupying(c, night)));
    }

    public String revenueReport(YearMonth month) {
        return Reports.revenue(db.transaction(c -> reservations.startingIn(c, month)));
    }

    @FunctionalInterface
    private interface AfterChange {
        void run(java.sql.Connection c, Reservation r) throws java.sql.SQLException;
    }

    private Reservation change(long id, Consumer<Reservation> operation, AfterChange after) {
        synchronized (writeLock) {
            try {
                return db.transaction(c -> {
                    Reservation r = reservations.find(c, id).orElseThrow(ApiException::reservationNotFound);
                    operation.accept(r);
                    reservations.update(c, r);
                    if (after != null) {
                        after.run(c, r);
                    }
                    return r;
                });
            } catch (OperationNotAllowedException e) {
                throw notAllowed(e);
            }
        }
    }

    private static ApiException notAllowed(OperationNotAllowedException e) {
        return new ApiException(409, "OPERATION_NOT_ALLOWED", e.getMessage());
    }
}
