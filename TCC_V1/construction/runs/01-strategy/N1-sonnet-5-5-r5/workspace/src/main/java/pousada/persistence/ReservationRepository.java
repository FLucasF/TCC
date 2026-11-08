package pousada.persistence;

import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {

    private static final String PAID = "('CONFIRMED','CHECKED_IN','CHECKED_OUT')";

    /** Trava as operações de reserva de um mesmo quarto até o fim da transação. */
    public void lockRoom(Connection c, int roomNumber) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            ps.setLong(1, roomNumber);
            ps.execute();
        }
    }

    /** Há reserva não cancelada do quarto em alguma noite de [checkIn, checkOut)? */
    public boolean hasOverlap(Connection c, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        String sql = "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                + "AND check_in < ? AND check_out > ? LIMIT 1";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Reservation insert(Connection c, Reservation r) throws SQLException {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, "
                + "check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, r);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return withId(r, keys.getLong(1));
            }
        }
    }

    public void update(Connection c, Reservation r) throws SQLException {
        String sql = "UPDATE reservations SET room_number=?, guest_name=?, guest_email=?, guest_cpf=?, "
                + "check_in=?, check_out=?, guests=?, rate_plan=?, nights=?, subtotal=?, discount=?, total=?, "
                + "status=?, amount_paid=?, refund=? WHERE id=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, r);
            ps.setLong(16, r.id());
            ps.executeUpdate();
        }
    }

    public Optional<Reservation> findById(Connection c, long id, boolean forUpdate) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE id = ?" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        }
    }

    /** Reservas pagas que ocupam um quarto na noite, por número do quarto. */
    public List<Reservation> occupying(Connection c, LocalDate night) throws SQLException {
        return query(c, "SELECT * FROM reservations WHERE status IN " + PAID
                + " AND check_in <= ? AND check_out > ? ORDER BY room_number, id",
                Date.valueOf(night), Date.valueOf(night));
    }

    /** Reservas que começam no período e deixaram dinheiro com a pousada. */
    public List<Reservation> withRevenueStartingBetween(Connection c, LocalDate from, LocalDate toExclusive)
            throws SQLException {
        return query(c, "SELECT * FROM reservations WHERE check_in >= ? AND check_in < ? "
                + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id",
                Date.valueOf(from), Date.valueOf(toExclusive));
    }

    private List<Reservation> query(Connection c, String sql, Date... dates) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < dates.length; i++) {
                ps.setDate(i + 1, dates[i]);
            }
            List<Reservation> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(read(rs));
                }
            }
            return result;
        }
    }

    private static void bind(PreparedStatement ps, Reservation r) throws SQLException {
        ps.setInt(1, r.roomNumber());
        ps.setString(2, r.guestName());
        ps.setString(3, r.guestEmail());
        ps.setString(4, r.guestCpf());
        ps.setDate(5, Date.valueOf(r.checkIn()));
        ps.setDate(6, Date.valueOf(r.checkOut()));
        ps.setInt(7, r.guests());
        ps.setString(8, r.ratePlan());
        ps.setInt(9, r.nights());
        ps.setBigDecimal(10, r.subtotal());
        ps.setBigDecimal(11, r.discount());
        ps.setBigDecimal(12, r.total());
        ps.setString(13, r.status().name());
        ps.setBigDecimal(14, r.amountPaid());
        ps.setBigDecimal(15, r.refund());
    }

    private static Reservation read(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getLong("id"),
                rs.getInt("room_number"),
                rs.getString("guest_name"),
                rs.getString("guest_email"),
                rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(),
                rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"),
                rs.getString("rate_plan"),
                rs.getInt("nights"),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }

    private static Reservation withId(Reservation r, long id) {
        return new Reservation(id, r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(), r.checkIn(),
                r.checkOut(), r.guests(), r.ratePlan(), r.nights(), r.subtotal(), r.discount(), r.total(),
                r.status(), r.amountPaid(), r.refund());
    }
}
