package br.com.marealta;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class ReservationService {
    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan) {}

    private static final String COLS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final Db db;
    private final MessageService messages;

    public ReservationService(Db db, MessageService messages) {
        this.db = db;
        this.messages = messages;
    }

    public List<Room> rooms() {
        return db.tx(c -> {
            List<Room> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(room(rs));
            }
            return out;
        });
    }

    public Reservation create(NewReservation n) {
        return db.tx(c -> {
            Room room = findRoom(c, n.roomNumber(), false);
            if (n.guests() > room.capacity()) {
                throw new ApiException(422, "CAPACITY_EXCEEDED",
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes");
            }
            findRoom(c, n.roomNumber(), true); // trava o quarto: duas reservas simultâneas não passam juntas
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                            + "AND check_in < ? AND check_out > ? LIMIT 1")) {
                ps.setInt(1, n.roomNumber());
                ps.setObject(2, n.checkOut());
                ps.setObject(3, n.checkIn());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        throw new ApiException(409, "ROOM_UNAVAILABLE", "O quarto já está reservado em alguma das noites");
                    }
                }
            }
            Pricing.Quote q = Pricing.quote(room.nightlyRate(), n.checkIn(), n.checkOut(), n.ratePlan());
            long id = Ids.next(c, "reservations");
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO reservations(" + COLS
                    + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NULL)")) {
                ps.setLong(1, id);
                ps.setInt(2, n.roomNumber());
                ps.setString(3, n.guestName());
                ps.setString(4, n.guestEmail());
                ps.setString(5, n.guestCpf());
                ps.setObject(6, n.checkIn());
                ps.setObject(7, n.checkOut());
                ps.setInt(8, n.guests());
                ps.setString(9, n.ratePlan());
                ps.setInt(10, q.nights());
                ps.setBigDecimal(11, q.subtotal());
                ps.setBigDecimal(12, q.discount());
                ps.setBigDecimal(13, q.total());
                ps.setString(14, "PENDING");
                ps.setBigDecimal(15, Money.ZERO);
                ps.executeUpdate();
            }
            return load(c, id, false);
        });
    }

    public Reservation get(long id) {
        return db.tx(c -> load(c, id, false));
    }

    public Reservation confirm(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "confirmar", "PENDING");
            update(c, id, "CONFIRMED", r.total(), null);
            Reservation confirmed = load(c, id, false);
            Receipt receipt = Receipt.of(confirmed, findRoom(c, confirmed.roomNumber(), false));
            messages.send(c, confirmed.guestEmail(), "Reserva confirmada - Pousada Maré Alta",
                    Documents.confirmationBody(receipt));
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "cancelar", "PENDING", "CONFIRMED");
            BigDecimal refund = r.ratePlan().equals("STANDARD") && r.status().equals("CONFIRMED")
                    ? r.amountPaid() : Money.ZERO;
            update(c, id, "CANCELLED", r.amountPaid(), refund);
            return load(c, id, false);
        });
    }

    public Reservation checkIn(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "fazer check-in de", "CONFIRMED");
            update(c, id, "CHECKED_IN", r.amountPaid(), r.refund());
            return load(c, id, false);
        });
    }

    public Receipt checkOut(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "fazer check-out de", "CHECKED_IN");
            update(c, id, "CHECKED_OUT", r.amountPaid(), r.refund());
            return Receipt.of(load(c, id, false), findRoom(c, r.roomNumber(), false));
        });
    }

    public Receipt document(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, false);
            if (!r.isPaid()) {
                throw ApiException.notAllowed("Só existe comprovante para reserva paga");
            }
            return Receipt.of(r, findRoom(c, r.roomNumber(), false));
        });
    }

    private static void requireStatus(Reservation r, String action, String... allowed) {
        for (String s : allowed) if (r.status().equals(s)) return;
        throw ApiException.notAllowed("Não é possível " + action + " uma reserva com situação " + r.status());
    }

    private static void update(Connection c, long id, String status, BigDecimal paid, BigDecimal refund) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setBigDecimal(2, paid);
            ps.setBigDecimal(3, refund);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    private static Room findRoom(Connection c, int number, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + number + " não existe");
                return room(rs);
            }
        }
    }

    private static Room room(ResultSet rs) throws SQLException {
        return new Room(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getBigDecimal(4));
    }

    private static Reservation load(Connection c, long id, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + COLS + " FROM reservations WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe");
                return reservation(rs);
            }
        }
    }

    static Reservation reservation(ResultSet rs) throws SQLException {
        return new Reservation(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getObject(6, LocalDate.class), rs.getObject(7, LocalDate.class), rs.getInt(8), rs.getString(9),
                rs.getInt(10), rs.getBigDecimal(11), rs.getBigDecimal(12), rs.getBigDecimal(13), rs.getString(14),
                rs.getBigDecimal(15), rs.getBigDecimal(16));
    }
}
