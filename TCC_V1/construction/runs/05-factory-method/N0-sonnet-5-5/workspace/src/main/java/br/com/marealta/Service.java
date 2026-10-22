package br.com.marealta;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

final class Service {
    private final Db db;

    Service(Db db) { this.db = db; }

    record NewReservation(int roomNumber, String guestName, String guestEmail, Identification identification,
                          LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    record IssuedDocument(Reservation reservation, Room room, Documents.Document document) {}

    private static final BigDecimal ZERO = Money.round(BigDecimal.ZERO);

    List<Room> rooms() {
        return db.tx(Repository::rooms);
    }

    List<Message> messages() {
        return db.tx(Repository::messages);
    }

    Reservation create(NewReservation n) {
        return db.tx(c -> {
            // o lock da linha do quarto serializa reservas concorrentes do mesmo quarto
            Room room = Repository.room(c, n.roomNumber(), true).orElseThrow(
                    () -> new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + n.roomNumber() + " não existe"));
            if (n.guests() > room.capacity()) {
                throw new ApiException(422, "CAPACITY_EXCEEDED",
                        "O quarto " + room.number() + " comporta até " + room.capacity() + " pessoas");
            }
            if (Repository.hasOverlap(c, room.number(), n.checkIn(), n.checkOut())) {
                throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
            }
            Pricing.Quote q = Pricing.quote(room.nightlyRate(), n.checkIn(), n.checkOut(), n.ratePlan());
            Reservation draft = new Reservation(0, room.number(), n.guestName(), n.guestEmail(), n.identification(),
                    n.checkIn(), n.checkOut(), n.guests(), n.ratePlan(), q.nights(), q.subtotal(), q.discount(),
                    q.total(), Status.PENDING, ZERO, null);
            long id = Repository.insert(c, draft);
            return Repository.reservation(c, id, false).orElseThrow();
        });
    }

    Reservation get(long id) {
        return db.tx(c -> load(c, id, false));
    }

    Reservation confirm(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "confirmar", Status.PENDING);
            Repository.updateState(c, id, Status.CONFIRMED, r.total(), null);
            Reservation confirmed = load(c, id, false);
            Room room = Repository.room(c, r.roomNumber(), false).orElseThrow();
            Documents.Document doc = Documents.render(confirmed, room);
            Repository.insertMessage(c, "EMAIL", confirmed.guestEmail(), Documents.confirmationSubject(),
                    Documents.confirmationBody(confirmed, doc));
            return confirmed;
        });
    }

    Reservation cancel(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "cancelar", Status.PENDING, Status.CONFIRMED);
            BigDecimal refund = r.status() == Status.CONFIRMED && r.ratePlan() == RatePlan.STANDARD
                    ? r.amountPaid() : ZERO;
            Repository.updateState(c, id, Status.CANCELLED, r.amountPaid(), refund);
            return load(c, id, false);
        });
    }

    Reservation checkIn(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "fazer check-in de", Status.CONFIRMED);
            Repository.updateState(c, id, Status.CHECKED_IN, r.amountPaid(), r.refund());
            return load(c, id, false);
        });
    }

    IssuedDocument checkOut(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "fazer check-out de", Status.CHECKED_IN);
            Repository.updateState(c, id, Status.CHECKED_OUT, r.amountPaid(), r.refund());
            return issue(c, load(c, id, false));
        });
    }

    /** Segunda via: só para reserva paga. */
    IssuedDocument document(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, false);
            if (!r.status().isPaid()) {
                throw ApiException.notAllowed("Só existe documento para reserva paga; a reserva está " + r.status());
            }
            return issue(c, r);
        });
    }

    String occupancyReport(LocalDate night) {
        return db.tx(c -> {
            List<Reservation> rows = Repository.reservations(c, """
                    WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') AND check_in <= ? AND check_out > ?
                    ORDER BY room_number, id""", night, night);
            List<Room> rooms = Repository.rooms(c);
            StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : rows) {
                Room room = rooms.stream().filter(x -> x.number() == r.roomNumber()).findFirst().orElseThrow();
                csv.append(r.roomNumber()).append(',').append(room.type()).append(',').append(r.id()).append(',')
                        .append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
            }
            long occupied = rows.stream().map(Reservation::roomNumber).distinct().count();
            csv.append("total,").append(occupied).append('/').append(rooms.size()).append('\n');
            return csv.toString();
        });
    }

    String revenueReport(YearMonth month) {
        return db.tx(c -> {
            List<Reservation> rows = Repository.reservations(c, """
                    WHERE check_in >= ? AND check_in <= ? AND amount_paid - COALESCE(refund, 0) > 0
                    ORDER BY check_in, id""", month.atDay(1), month.atEndOfMonth());
            StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal sum = ZERO;
            for (Reservation r : rows) {
                BigDecimal kept = r.amountPaid().subtract(r.refund() == null ? ZERO : r.refund());
                sum = sum.add(kept);
                csv.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                        .append(r.checkOut()).append(',').append(r.status()).append(',').append(kept).append('\n');
            }
            csv.append("total,").append(sum).append('\n');
            return csv.toString();
        });
    }

    private static Reservation load(java.sql.Connection c, long id, boolean lock) throws SQLException {
        return Repository.reservation(c, id, lock).orElseThrow(
                () -> new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe"));
    }

    private static IssuedDocument issue(java.sql.Connection c, Reservation r) throws SQLException {
        Room room = Repository.room(c, r.roomNumber(), false).orElseThrow();
        return new IssuedDocument(r, room, Documents.render(r, room));
    }

    private static void requireStatus(Reservation r, String action, Status... allowed) {
        for (Status s : allowed) if (r.status() == s) return;
        throw ApiException.notAllowed("Não é possível " + action + " uma reserva com status " + r.status());
    }
}
