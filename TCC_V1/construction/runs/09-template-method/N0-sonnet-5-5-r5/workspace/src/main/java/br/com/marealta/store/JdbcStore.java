package br.com.marealta.store;

import br.com.marealta.domain.Message;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import br.com.marealta.domain.Status;
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
import java.util.function.Function;
import javax.sql.DataSource;

public class JdbcStore implements Store {
    private static final String RESERVATION_COLUMNS =
            "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights,"
            + " subtotal, discount, total, status, amount_paid, refund";

    private final DataSource dataSource;

    public JdbcStore(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Cria o que faltar no banco e cadastra os quartos se a tabela estiver vazia. */
    public void initialize(List<Room> initialRooms) {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try (Statement st = c.createStatement()) {
                // Serializa a inicialização caso mais de uma instância suba junto.
                st.execute("SELECT pg_advisory_xact_lock(7070001)");
                st.execute("CREATE TABLE IF NOT EXISTS rooms ("
                        + "number INTEGER PRIMARY KEY, type VARCHAR(20) NOT NULL,"
                        + " capacity INTEGER NOT NULL, nightly_rate NUMERIC(12,2) NOT NULL)");
                st.execute("CREATE TABLE IF NOT EXISTS reservations ("
                        + "id BIGSERIAL PRIMARY KEY,"
                        + " room_number INTEGER NOT NULL REFERENCES rooms(number),"
                        + " guest_name TEXT NOT NULL, guest_email TEXT NOT NULL, guest_cpf TEXT NOT NULL,"
                        + " check_in DATE NOT NULL, check_out DATE NOT NULL, guests INTEGER NOT NULL,"
                        + " rate_plan VARCHAR(20) NOT NULL, nights INTEGER NOT NULL,"
                        + " subtotal NUMERIC(12,2) NOT NULL, discount NUMERIC(12,2) NOT NULL,"
                        + " total NUMERIC(12,2) NOT NULL, status VARCHAR(20) NOT NULL,"
                        + " amount_paid NUMERIC(12,2) NOT NULL, refund NUMERIC(12,2))");
                st.execute("CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations(room_number, check_in)");
                st.execute("CREATE INDEX IF NOT EXISTS reservations_check_in_idx ON reservations(check_in)");
                st.execute("CREATE TABLE IF NOT EXISTS messages ("
                        + "id BIGSERIAL PRIMARY KEY, channel VARCHAR(20) NOT NULL, recipient TEXT NOT NULL,"
                        + " subject TEXT NOT NULL, body TEXT NOT NULL)");
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)"
                            + " ON CONFLICT (number) DO NOTHING")) {
                for (Room r : initialRooms) {
                    ps.setInt(1, r.number());
                    ps.setString(2, r.type().name());
                    ps.setInt(3, r.capacity());
                    ps.setBigDecimal(4, r.nightlyRate());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            c.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao preparar o banco: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T tx(Function<Tx, T> work) {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.apply(new JdbcTx(c));
                c.commit();
                return result;
            } catch (RuntimeException | Error e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class JdbcTx implements Tx {
        private final Connection c;

        JdbcTx(Connection c) { this.c = c; }

        @Override
        public List<Room> rooms() {
            return query("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number",
                    ps -> {}, JdbcTx::mapRoom);
        }

        @Override
        public Optional<Room> room(int number, boolean lock) {
            return query("SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?"
                            + (lock ? " FOR UPDATE" : ""),
                    ps -> ps.setInt(1, number), JdbcTx::mapRoom).stream().findFirst();
        }

        @Override
        public boolean overlaps(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
            return !query("SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED'"
                            + " AND check_in < ? AND ? < check_out LIMIT 1",
                    ps -> {
                        ps.setInt(1, roomNumber);
                        ps.setDate(2, Date.valueOf(checkOut));
                        ps.setDate(3, Date.valueOf(checkIn));
                    }, rs -> 1).isEmpty();
        }

        @Override
        public Reservation insert(Reservation r) {
            String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in,"
                    + " check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)"
                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            return query(sql, ps -> {
                ps.setInt(1, r.roomNumber());
                ps.setString(2, r.guestName());
                ps.setString(3, r.guestEmail());
                ps.setString(4, r.guestCpf());
                ps.setDate(5, Date.valueOf(r.checkIn()));
                ps.setDate(6, Date.valueOf(r.checkOut()));
                ps.setInt(7, r.guests());
                ps.setString(8, r.ratePlan().name());
                ps.setInt(9, r.nights());
                ps.setBigDecimal(10, r.subtotal());
                ps.setBigDecimal(11, r.discount());
                ps.setBigDecimal(12, r.total());
                ps.setString(13, r.status().name());
                ps.setBigDecimal(14, r.amountPaid());
                setRefund(ps, 15, r.refund());
            }, rs -> r.withId(rs.getLong(1))).get(0);
        }

        @Override
        public Optional<Reservation> reservation(long id, boolean lock) {
            return query("SELECT " + RESERVATION_COLUMNS + " FROM reservations WHERE id = ?"
                            + (lock ? " FOR UPDATE" : ""),
                    ps -> ps.setLong(1, id), JdbcTx::mapReservation).stream().findFirst();
        }

        @Override
        public void update(Reservation r) {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
                ps.setString(1, r.status().name());
                ps.setBigDecimal(2, r.amountPaid());
                setRefund(ps, 3, r.refund());
                ps.setLong(4, r.id());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Message addMessage(String channel, String to, String subject, String body) {
            return query("INSERT INTO messages (channel, recipient, subject, body) VALUES (?, ?, ?, ?) RETURNING id",
                    ps -> {
                        ps.setString(1, channel);
                        ps.setString(2, to);
                        ps.setString(3, subject);
                        ps.setString(4, body);
                    }, rs -> new Message(rs.getLong(1), channel, to, subject, body)).get(0);
        }

        @Override
        public List<Message> messages() {
            return query("SELECT id, channel, recipient, subject, body FROM messages ORDER BY id", ps -> {},
                    rs -> new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getString(5)));
        }

        @Override
        public List<Reservation> startingBetween(LocalDate from, LocalDate toExclusive) {
            return query("SELECT " + RESERVATION_COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ?"
                            + " ORDER BY check_in, id",
                    ps -> {
                        ps.setDate(1, Date.valueOf(from));
                        ps.setDate(2, Date.valueOf(toExclusive));
                    }, JdbcTx::mapReservation);
        }

        @Override
        public List<Reservation> stayingOn(LocalDate night) {
            return query("SELECT " + RESERVATION_COLUMNS + " FROM reservations WHERE check_in <= ? AND ? < check_out"
                            + " ORDER BY room_number, id",
                    ps -> {
                        ps.setDate(1, Date.valueOf(night));
                        ps.setDate(2, Date.valueOf(night));
                    }, JdbcTx::mapReservation);
        }

        private static void setRefund(PreparedStatement ps, int index, BigDecimal refund) throws SQLException {
            if (refund == null) {
                ps.setNull(index, Types.NUMERIC);
            } else {
                ps.setBigDecimal(index, refund);
            }
        }

        private static Room mapRoom(ResultSet rs) throws SQLException {
            return new Room(rs.getInt(1), RoomType.valueOf(rs.getString(2)), rs.getInt(3), rs.getBigDecimal(4));
        }

        private static Reservation mapReservation(ResultSet rs) throws SQLException {
            return new Reservation(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
                    rs.getDate(6).toLocalDate(), rs.getDate(7).toLocalDate(), rs.getInt(8),
                    RatePlan.valueOf(rs.getString(9)), rs.getInt(10), rs.getBigDecimal(11), rs.getBigDecimal(12),
                    rs.getBigDecimal(13), Status.valueOf(rs.getString(14)), rs.getBigDecimal(15),
                    rs.getBigDecimal(16));
        }

        private <T> List<T> query(String sql, Binder binder, RowMapper<T> mapper) {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                binder.bind(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    List<T> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(mapper.map(rs));
                    }
                    return out;
                }
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @FunctionalInterface
    private interface Binder { void bind(PreparedStatement ps) throws SQLException; }

    @FunctionalInterface
    private interface RowMapper<T> { T map(ResultSet rs) throws SQLException; }
}
