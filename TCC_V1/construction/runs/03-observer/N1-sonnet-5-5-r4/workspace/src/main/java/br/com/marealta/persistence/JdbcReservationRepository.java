package br.com.marealta.persistence;

import br.com.marealta.app.ReservationRepository;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcReservationRepository implements ReservationRepository {
    private static final String COLUMNS = """
            room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights,
            subtotal, discount, total, status, amount_paid, refund""";

    private final DataSource dataSource;

    public JdbcReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Reservation insert(Reservation r) {
        String sql = "INSERT INTO reservations (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, r);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return r.withId(keys.getLong("id"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void update(Reservation r) {
        String sql = "UPDATE reservations SET room_number=?, guest_name=?, guest_email=?, guest_cpf=?, check_in=?,"
                + " check_out=?, guests=?, rate_plan=?, nights=?, subtotal=?, discount=?, total=?, status=?,"
                + " amount_paid=?, refund=? WHERE id=?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, r);
            ps.setLong(16, r.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Optional<Reservation> find(long id) {
        List<Reservation> found = query("SELECT id, " + COLUMNS + " FROM reservations WHERE id=?", ps -> ps.setLong(1, id));
        return found.stream().findFirst();
    }

    @Override
    public List<Reservation> findAll() {
        return query("SELECT id, " + COLUMNS + " FROM reservations ORDER BY id", ps -> { });
    }

    @Override
    public boolean existsActiveOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = "SELECT 1 FROM reservations WHERE room_number=? AND status<>'CANCELLED'"
                + " AND check_in < ? AND ? < check_out LIMIT 1";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Reservation> query(String sql, Binder binder) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(read(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void bind(PreparedStatement ps, Reservation r) throws SQLException {
        ps.setInt(1, r.roomNumber());
        ps.setString(2, r.guestName());
        ps.setString(3, r.guestEmail());
        ps.setString(4, r.guestCpf());
        ps.setObject(5, r.checkIn());
        ps.setObject(6, r.checkOut());
        ps.setInt(7, r.guests());
        ps.setString(8, r.ratePlan().name());
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
                rs.getObject("check_in", LocalDate.class),
                rs.getObject("check_out", LocalDate.class),
                rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")),
                rs.getInt("nights"),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
