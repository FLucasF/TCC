package br.com.marealta.domain;

import br.com.marealta.Db;
import br.com.marealta.mail.Mailer;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ReservationService {
    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    private static final String COLS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, "
            + "rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";
    private static final String SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    private final Db db;
    private final Mailer mailer;
    /** Serializa as operações de escrita desta instalação (regras de disponibilidade e limites de mensagem). */
    private final Object lock = new Object();

    public ReservationService(Db db, Mailer mailer) {
        this.db = db;
        this.mailer = mailer;
    }

    public List<Room> rooms() {
        return db.tx(c -> {
            List<Room> rooms = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rooms.add(roomOf(rs));
            }
            return rooms;
        });
    }

    public Reservation create(NewReservation n) {
        synchronized (lock) {
            return db.tx(c -> {
                Room room = findRoom(c, n.roomNumber());
                if (room == null) throw new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + n.roomNumber() + " não existe");
                if (n.guests() > room.capacity()) {
                    throw new ApiException(422, "CAPACITY_EXCEEDED",
                            "O quarto " + room.number() + " comporta até " + room.capacity() + " pessoas");
                }
                if (isTaken(c, room.number(), n.checkIn(), n.checkOut())) {
                    throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites pedidas");
                }
                Pricing.Quote q = Pricing.quote(room, n.checkIn(), n.checkOut(), n.ratePlan());
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO reservations(room_number, guest_name, guest_email, "
                        + "guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,'PENDING',0) RETURNING " + COLS)) {
                    ps.setInt(1, room.number());
                    ps.setString(2, n.guestName());
                    ps.setString(3, n.guestEmail());
                    ps.setString(4, n.guestCpf());
                    ps.setDate(5, Date.valueOf(n.checkIn()));
                    ps.setDate(6, Date.valueOf(n.checkOut()));
                    ps.setInt(7, n.guests());
                    ps.setString(8, n.ratePlan().name());
                    ps.setInt(9, q.nights());
                    ps.setBigDecimal(10, q.subtotal());
                    ps.setBigDecimal(11, q.discount());
                    ps.setBigDecimal(12, q.total());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        return reservationOf(rs);
                    }
                }
            });
        }
    }

    public Reservation get(long id) {
        return db.tx(c -> load(c, id, false));
    }

    public Reservation confirm(long id) {
        synchronized (lock) {
            return db.tx(c -> {
                Reservation r = load(c, id, true);
                if (r.status() != Status.PENDING) throw ApiException.notAllowed("Só é possível confirmar uma reserva pendente");
                Reservation updated = r.with(Status.CONFIRMED, r.total(), null);
                save(c, updated);
                Room room = findRoom(c, r.roomNumber());
                String receipt = Receipt.of(updated, room).toText();
                String body = "Olá, " + r.guestName() + "!\n\n"
                        + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                        + receipt + "\n\n"
                        + "A entrada é a partir das 14h, e a saída, até as 12h.\nAté breve!";
                mailer.send(c, r.guestEmail(), SUBJECT, body);
                return updated;
            });
        }
    }

    public Reservation cancel(long id) {
        synchronized (lock) {
            return db.tx(c -> {
                Reservation r = load(c, id, true);
                if (r.status() != Status.PENDING && r.status() != Status.CONFIRMED) {
                    throw ApiException.notAllowed("Só é possível cancelar antes do check-in");
                }
                BigDecimal refund = r.ratePlan() == RatePlan.STANDARD
                        ? r.amountPaid() : Pricing.round(BigDecimal.ZERO);
                Reservation updated = r.with(Status.CANCELLED, r.amountPaid(), refund);
                save(c, updated);
                return updated;
            });
        }
    }

    public Reservation checkIn(long id) {
        synchronized (lock) {
            return db.tx(c -> {
                Reservation r = load(c, id, true);
                if (r.status() != Status.CONFIRMED) throw ApiException.notAllowed("Só é possível fazer check-in de uma reserva confirmada");
                Reservation updated = r.with(Status.CHECKED_IN, r.amountPaid(), r.refund());
                save(c, updated);
                return updated;
            });
        }
    }

    public Receipt checkOut(long id) {
        synchronized (lock) {
            return db.tx(c -> {
                Reservation r = load(c, id, true);
                if (r.status() != Status.CHECKED_IN) throw ApiException.notAllowed("Só é possível fazer check-out de quem fez check-in");
                Reservation updated = r.with(Status.CHECKED_OUT, r.amountPaid(), r.refund());
                save(c, updated);
                return Receipt.of(updated, findRoom(c, r.roomNumber()));
            });
        }
    }

    public Receipt document(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, false);
            if (!r.status().isPaid()) throw ApiException.notAllowed("A segunda via só existe para reserva paga");
            return Receipt.of(r, findRoom(c, r.roomNumber()));
        });
    }

    // ---- acesso a dados (também usado pelos relatórios) ----

    static Room findRoom(Connection c, int number) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? roomOf(rs) : null;
            }
        }
    }

    private static boolean isTaken(Connection c, int room, LocalDate in, LocalDate out) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM reservations WHERE room_number = ? "
                + "AND status <> 'CANCELLED' AND check_in < ? AND check_out > ? LIMIT 1")) {
            ps.setInt(1, room);
            ps.setDate(2, Date.valueOf(out));
            ps.setDate(3, Date.valueOf(in));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static Reservation load(Connection c, long id, boolean forUpdate) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + COLS + " FROM reservations WHERE id = ?" + (forUpdate ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe");
                return reservationOf(rs);
            }
        }
    }

    private static void save(Connection c, Reservation r) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        }
    }

    static Room roomOf(ResultSet rs) throws SQLException {
        return new Room(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getBigDecimal(4));
    }

    static Reservation reservationOf(ResultSet rs) throws SQLException {
        return new Reservation(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getDate(6).toLocalDate(), rs.getDate(7).toLocalDate(), rs.getInt(8),
                RatePlan.valueOf(rs.getString(9)), rs.getInt(10), rs.getBigDecimal(11), rs.getBigDecimal(12),
                rs.getBigDecimal(13), Status.valueOf(rs.getString(14)), rs.getBigDecimal(15), rs.getBigDecimal(16));
    }
}
