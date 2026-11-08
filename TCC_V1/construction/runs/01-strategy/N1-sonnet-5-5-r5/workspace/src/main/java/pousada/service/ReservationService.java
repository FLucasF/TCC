package pousada.service;

import pousada.domain.Booking;
import pousada.domain.DomainException;
import pousada.domain.Message;
import pousada.domain.Money;
import pousada.domain.Quote;
import pousada.domain.RatePlan;
import pousada.domain.RatePlans;
import pousada.domain.Receipt;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;
import pousada.domain.Room;
import pousada.domain.Rooms;
import pousada.persistence.Database;
import pousada.persistence.MessageRepository;
import pousada.persistence.ReservationRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.function.UnaryOperator;

public class ReservationService {
    private final Database database;
    private final ReservationRepository reservations = new ReservationRepository();
    private final MessageRepository messages = new MessageRepository();

    public ReservationService(Database database) {
        this.database = database;
    }

    public Reservation create(CreateReservation cmd) {
        Room room = Rooms.find(cmd.roomNumber()).orElseThrow(() -> new DomainException(
                404, "ROOM_NOT_FOUND", "Quarto " + cmd.roomNumber() + " não existe."));
        if (cmd.guests() > room.capacity()) {
            throw new DomainException(422, "CAPACITY_EXCEEDED",
                    "O quarto " + room.number() + " comporta até " + room.capacity() + " pessoas.");
        }
        RatePlan plan = RatePlans.find(cmd.ratePlan()).orElseThrow(
                () -> DomainException.invalidRequest("Plano de tarifa desconhecido: " + cmd.ratePlan()));
        Booking booking = new Booking(room, cmd.checkIn(), cmd.checkOut(), cmd.guests(), cmd.companyCode());
        Quote quote = plan.quote(booking);

        return database.inTransaction(c -> {
            reservations.lockRoom(c, room.number());
            if (reservations.hasOverlap(c, room.number(), cmd.checkIn(), cmd.checkOut())) {
                throw new DomainException(409, "ROOM_UNAVAILABLE",
                        "O quarto " + room.number() + " já está reservado em alguma das noites.");
            }
            return reservations.insert(c, new Reservation(0, room.number(), cmd.guestName(), cmd.guestEmail(),
                    cmd.guestCpf(), cmd.checkIn(), cmd.checkOut(), cmd.guests(), plan.code(), booking.nights(),
                    quote.subtotal(), quote.discount(), quote.total(), ReservationStatus.PENDING,
                    Money.ZERO, null));
        });
    }

    public Reservation get(long id) {
        return database.inTransaction(c -> load(c, id, false));
    }

    public Reservation confirm(long id) {
        return database.inTransaction(c -> {
            Reservation confirmed = apply(c, id, Reservation::confirm);
            Receipt receipt = confirmed.receipt(roomOf(confirmed));
            messages.insert(c, Message.confirmationEmail(confirmed, receipt));
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return database.inTransaction(c -> apply(c, id, Reservation::cancel));
    }

    public Reservation checkIn(long id) {
        return database.inTransaction(c -> apply(c, id, Reservation::startStay));
    }

    public Receipt checkOut(long id) {
        return database.inTransaction(c -> {
            Reservation done = apply(c, id, Reservation::endStay);
            return done.receipt(roomOf(done));
        });
    }

    public Receipt document(long id) {
        return database.inTransaction(c -> {
            Reservation r = load(c, id, false);
            return r.receipt(roomOf(r));
        });
    }

    public List<Message> messages() {
        return database.inTransaction(messages::findAll);
    }

    public String occupancyReport(LocalDate night) {
        return database.inTransaction(c -> Reports.occupancy(reservations.occupying(c, night)));
    }

    public String revenueReport(YearMonth month) {
        return database.inTransaction(c -> Reports.revenue(
                reservations.withRevenueStartingBetween(c, month.atDay(1), month.plusMonths(1).atDay(1))));
    }

    private Reservation apply(Connection c, long id, UnaryOperator<Reservation> transition) throws SQLException {
        Reservation updated = transition.apply(load(c, id, true));
        reservations.update(c, updated);
        return updated;
    }

    private Reservation load(Connection c, long id, boolean forUpdate) throws SQLException {
        return reservations.findById(c, id, forUpdate).orElseThrow(() -> new DomainException(
                404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe."));
    }

    private static Room roomOf(Reservation r) {
        return Rooms.find(r.roomNumber()).orElseThrow();
    }
}
