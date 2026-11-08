package br.com.marealta;

import br.com.marealta.plan.PricingContext;
import br.com.marealta.plan.Quote;
import br.com.marealta.plan.RatePlan;
import br.com.marealta.plan.RatePlans;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public final class ReservationService {
    public record CreateCommand(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan, String companyCode) {}

    /** Reserva junto com o quarto, para montar respostas e comprovantes. */
    public record Booking(Reservation reservation, Room room) {}

    private final Db db;
    private final Repository repo;

    public ReservationService(Db db, Repository repo) {
        this.db = db;
        this.repo = repo;
    }

    public List<Room> rooms() {
        return db.tx(repo::rooms);
    }

    public List<Message> messages() {
        return db.tx(repo::messages);
    }

    public Reservation create(CreateCommand cmd) {
        return db.tx(c -> {
            // O lock do quarto serializa reservas concorrentes para o mesmo quarto.
            Room room = repo.room(c, cmd.roomNumber(), true)
                    .orElseThrow(() -> ApiException.roomNotFound("Quarto " + cmd.roomNumber() + " não existe."));
            if (cmd.guests() > room.capacity()) {
                throw ApiException.capacityExceeded("O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes.");
            }
            RatePlan plan = RatePlans.find(cmd.ratePlan())
                    .orElseThrow(() -> ApiException.invalid("ratePlan desconhecido: " + cmd.ratePlan()));
            PricingContext ctx = new PricingContext(room, cmd.checkIn(), cmd.checkOut(), cmd.companyCode());
            Quote quote = plan.quote(ctx);
            if (repo.hasOverlap(c, room.number(), cmd.checkIn(), cmd.checkOut())) {
                throw ApiException.roomUnavailable("O quarto " + room.number() + " já está reservado em alguma das noites.");
            }
            boolean corporate = "CORPORATE".equals(plan.code());
            Reservation draft = new Reservation(0, room.number(), cmd.guestName(), cmd.guestEmail(), cmd.guestCpf(),
                    cmd.checkIn(), cmd.checkOut(), cmd.guests(), plan.code(), corporate ? cmd.companyCode().trim() : null,
                    ctx.nights(), quote.subtotal(), quote.discount(), quote.total(),
                    ReservationStatus.PENDING, Money.ZERO, null);
            return repo.insert(c, draft);
        });
    }

    public Reservation get(long id) {
        return db.tx(c -> load(c, id, false));
    }

    public Reservation confirm(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            require(r.status() == ReservationStatus.PENDING, "Só é possível confirmar uma reserva pendente.");
            Reservation updated = repo.update(c, id, ReservationStatus.CONFIRMED, r.total(), r.refund());
            Room room = repo.room(c, r.roomNumber(), false).orElseThrow();
            repo.insertMessage(c, new Message(0, "EMAIL", updated.guestEmail(), Receipt.CONFIRMATION_SUBJECT,
                    Receipt.confirmationEmail(updated, room)));
            return updated;
        });
    }

    public Reservation cancel(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            require(r.status() == ReservationStatus.PENDING || r.status() == ReservationStatus.CONFIRMED,
                    "Só é possível cancelar uma reserva pendente ou confirmada.");
            RatePlan plan = RatePlans.find(r.ratePlan()).orElseThrow();
            BigDecimal refund = Money.round(plan.refund(r.amountPaid()));
            return repo.update(c, id, ReservationStatus.CANCELLED, r.amountPaid(), refund);
        });
    }

    public Reservation checkIn(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            require(r.status() == ReservationStatus.CONFIRMED, "Só é possível fazer check-in de uma reserva confirmada.");
            return repo.update(c, id, ReservationStatus.CHECKED_IN, r.amountPaid(), r.refund());
        });
    }

    public Booking checkOut(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            require(r.status() == ReservationStatus.CHECKED_IN, "Só é possível fazer check-out de quem fez check-in.");
            Reservation updated = repo.update(c, id, ReservationStatus.CHECKED_OUT, r.amountPaid(), r.refund());
            return new Booking(updated, repo.room(c, r.roomNumber(), false).orElseThrow());
        });
    }

    /** Segunda via do comprovante: só para reserva paga. */
    public Booking document(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, false);
            require(r.status().isPaid(), "O comprovante só existe para reserva paga.");
            return new Booking(r, repo.room(c, r.roomNumber(), false).orElseThrow());
        });
    }

    private Reservation load(Connection c, long id, boolean lock) throws SQLException {
        return repo.find(c, id, lock)
                .orElseThrow(() -> ApiException.reservationNotFound("Reserva " + id + " não existe."));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw ApiException.notAllowed(message);
    }
}
