package pousada.repo;

import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcReservationRepository implements ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,"
            + " guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final DataSource ds;

    public JdbcReservationRepository(DataSource ds) {
        this.ds = ds;
    }

    @Override
    public Reservation insert(Reservation r) {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,"
                + " guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, r, 1);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                r.assignId(rs.getLong(1));
            }
            return r;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void update(Reservation r) {
        String sql = "UPDATE reservations SET room_number=?, guest_name=?, guest_email=?, guest_cpf=?, check_in=?,"
                + " check_out=?, guests=?, rate_plan=?, nights=?, subtotal=?, discount=?, total=?, status=?,"
                + " amount_paid=?, refund=? WHERE id=?";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, r, 1);
            ps.setLong(16, r.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<Reservation> find(long id) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE id=?", ps -> ps.setLong(1, id))
                .stream().findFirst();
    }

    @Override
    public boolean hasOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = "SELECT 1 FROM reservations WHERE room_number=? AND status<>'CANCELLED'"
                + " AND check_in < ? AND check_out > ? LIMIT 1";
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            ps.setDate(2, Date.valueOf(checkOut));
            ps.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Reservation> findCheckInBetween(LocalDate from, LocalDate to) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in <= ?"
                + " ORDER BY check_in, id", ps -> {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
        });
    }

    @Override
    public List<Reservation> findPaidOccupying(LocalDate night) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT')"
                + " AND check_in <= ? AND check_out > ? ORDER BY room_number, id", ps -> {
            ps.setDate(1, Date.valueOf(night));
            ps.setDate(2, Date.valueOf(night));
        });
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Reservation> query(String sql, Binder binder) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(read(rs));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void bind(PreparedStatement ps, Reservation r, int i) throws SQLException {
        ps.setInt(i++, r.roomNumber());
        ps.setString(i++, r.guestName());
        ps.setString(i++, r.guestEmail());
        ps.setString(i++, r.guestCpf());
        ps.setDate(i++, Date.valueOf(r.checkIn()));
        ps.setDate(i++, Date.valueOf(r.checkOut()));
        ps.setInt(i++, r.guests());
        ps.setString(i++, r.ratePlan().name());
        ps.setInt(i++, r.nights());
        ps.setBigDecimal(i++, r.subtotal());
        ps.setBigDecimal(i++, r.discount());
        ps.setBigDecimal(i++, r.total());
        ps.setString(i++, r.status().name());
        ps.setBigDecimal(i++, r.amountPaid());
        if (r.refund() == null) {
            ps.setNull(i, Types.NUMERIC);
        } else {
            ps.setBigDecimal(i, r.refund());
        }
    }

    private static Reservation read(ResultSet rs) throws SQLException {
        BigDecimal refund = rs.getBigDecimal("refund");
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), rs.getInt("nights"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"), refund);
    }
}
