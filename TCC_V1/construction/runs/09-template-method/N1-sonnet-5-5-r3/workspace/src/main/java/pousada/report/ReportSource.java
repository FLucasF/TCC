package pousada.report;

import pousada.Db;
import pousada.Reservation;
import pousada.ReservationRepository;
import pousada.ReservationStatus;
import pousada.Room;
import pousada.RoomRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Consultas compartilhadas pelos relatórios. */
public final class ReportSource {
    private final Db db;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;

    public ReportSource(Db db, RoomRepository rooms, ReservationRepository reservations) {
        this.db = db;
        this.rooms = rooms;
        this.reservations = reservations;
    }

    public List<Room> rooms() {
        return db.tx(rooms::all);
    }

    /** Reservas pagas e não canceladas que ocupam um quarto na noite. */
    public List<Reservation> paidOccupying(LocalDate night) {
        return db.tx(c -> reservations.occupying(c, night)).stream()
                .filter(r -> r.status().isPaidStay()).toList();
    }

    /** Reservas pagas e não canceladas que começam no mês. */
    public List<Reservation> paidStartingIn(YearMonth month) {
        return db.tx(c -> reservations.startingIn(c, month)).stream()
                .filter(r -> r.status().isPaidStay()).toList();
    }

    /** Reservas do mês que deixaram dinheiro com a pousada (pago menos devolvido). */
    public List<Reservation> retainedStartingIn(YearMonth month) {
        return db.tx(c -> reservations.startingIn(c, month)).stream()
                .filter(r -> r.retained().signum() > 0).toList();
    }
}
