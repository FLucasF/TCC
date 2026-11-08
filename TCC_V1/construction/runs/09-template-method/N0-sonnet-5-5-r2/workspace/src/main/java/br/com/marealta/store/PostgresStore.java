package br.com.marealta.store;

import br.com.marealta.domain.Message;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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
import java.util.function.Function;

public class PostgresStore implements Store, AutoCloseable {
    private static final String RESERVATION_COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, "
            + "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, "
            + "amount_paid, refund";

    private final HikariDataSource dataSource;

    public PostgresStore(String url, String user, String password, List<Room> catalog) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setInitializationFailTimeout(60_000); // o banco pode demorar a subir
        this.dataSource = new HikariDataSource(config);
        migrate(catalog);
    }

    private void migrate(List<Room> catalog) {
        transaction(tx -> {
            Connection c = ((PgTx) tx).connection;
            try (Statement st = c.createStatement()) {
                // evita corrida entre instâncias subindo juntas
                st.execute("SELECT pg_advisory_xact_lock(7070001)");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS rooms (
                            number INTEGER PRIMARY KEY,
                            type VARCHAR(20) NOT NULL,
                            capacity INTEGER NOT NULL,
                            nightly_rate NUMERIC(12,2) NOT NULL)""");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS reservations (
                            id BIGSERIAL PRIMARY KEY,
                            room_number INTEGER NOT NULL REFERENCES rooms(number),
                            guest_name TEXT NOT NULL,
                            guest_email TEXT NOT NULL,
                            guest_cpf TEXT NOT NULL,
                            check_in DATE NOT NULL,
                            check_out DATE NOT NULL,
                            guests INTEGER NOT NULL,
                            rate_plan VARCHAR(20) NOT NULL,
                            nights INTEGER NOT NULL,
                            subtotal NUMERIC(12,2) NOT NULL,
                            discount NUMERIC(12,2) NOT NULL,
                            total NUMERIC(12,2) NOT NULL,
                            status VARCHAR(20) NOT NULL,
                            amount_paid NUMERIC(12,2) NOT NULL,
                            refund NUMERIC(12,2))""");
                st.execute("CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations(room_number, check_in)");
                st.execute("CREATE INDEX IF NOT EXISTS reservations_check_in_idx ON reservations(check_in)");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS messages (
                            id BIGSERIAL PRIMARY KEY,
                            channel VARCHAR(20) NOT NULL,
                            recipient TEXT NOT NULL,
                            subject TEXT NOT NULL,
                            body TEXT NOT NULL)""");
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO rooms(number, type, capacity, nightly_rate) VALUES (?,?,?,?) "
                            + "ON CONFLICT (number) DO NOTHING")) {
                for (Room r : catalog) {
                    ps.setInt(1, r.number());
                    ps.setString(2, r.type().name());
                    ps.setInt(3, r.capacity());
                    ps.setBigDecimal(4, r.nightlyRate());
                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            return null;
        });
    }

    @Override
    public <T> T transaction(Function<Tx, T> work) {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.apply(new PgTx(c));
                c.commit();
                return result;
            } catch (Throwable t) {
                try { c.rollback(); } catch (SQLException ignored) { }
                throw t;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private static final class PgTx implements Tx {
        final Connection connection;

        PgTx(Connection connection) { this.connection = connection; }

        private <T> T sql(SqlWork<T> work) {
            try {
                return work.run(connection);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public List<Room> rooms() {
            return sql(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
                     ResultSet rs = ps.executeQuery()) {
                    List<Room> list = new ArrayList<>();
                    while (rs.next()) list.add(room(rs));
                    return list;
                }
            });
        }

        @Override
        public Optional<Room> room(int number) {
            return sql(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {
                    ps.setInt(1, number);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() ? Optional.of(room(rs)) : Optional.empty();
                    }
                }
            });
        }

        private static Room room(ResultSet rs) throws SQLException {
            return new Room(rs.getInt(1), RoomType.valueOf(rs.getString(2)), rs.getInt(3), rs.getBigDecimal(4));
        }

        @Override
        public void lockRoom(int number) {
            sql(c -> {
                try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM rooms WHERE number = ? FOR UPDATE")) {
                    ps.setInt(1, number);
                    ps.executeQuery().close();
                }
                return null;
            });
        }

        @Override
        public boolean hasActiveOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
            return sql(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                                + "AND check_in < ? AND check_out > ? LIMIT 1")) {
                    ps.setInt(1, roomNumber);
                    ps.setDate(2, Date.valueOf(checkOut));
                    ps.setDate(3, Date.valueOf(checkIn));
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next();
                    }
                }
            });
        }

        @Override
        public Reservation insert(Reservation d) {
            return sql(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO reservations(room_number, guest_name, guest_email, guest_cpf, check_in, "
                                + "check_out, guests, rate_plan, nights, subtotal, discount, total, status, "
                                + "amount_paid, refund) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id")) {
                    ps.setInt(1, d.roomNumber());
                    ps.setString(2, d.guestName());
                    ps.setString(3, d.guestEmail());
                    ps.setString(4, d.guestCpf());
                    ps.setDate(5, Date.valueOf(d.checkIn()));
                    ps.setDate(6, Date.valueOf(d.checkOut()));
                    ps.setInt(7, d.guests());
                    ps.setString(8, d.ratePlan().name());
                    ps.setInt(9, d.nights());
                    ps.setBigDecimal(10, d.subtotal());
                    ps.setBigDecimal(11, d.discount());
                    ps.setBigDecimal(12, d.total());
                    ps.setString(13, d.status().name());
                    ps.setBigDecimal(14, d.amountPaid());
                    ps.setBigDecimal(15, d.refund());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        long id = rs.getLong(1);
                        return new Reservation(id, d.roomNumber(), d.guestName(), d.guestEmail(), d.guestCpf(),
                                d.checkIn(), d.checkOut(), d.guests(), d.ratePlan(), d.nights(), d.subtotal(),
                                d.discount(), d.total(), d.status(), d.amountPaid(), d.refund());
                    }
                }
            });
        }

        @Override
        public Optional<Reservation> reservationForUpdate(long id) {
            return findReservation(id, " FOR UPDATE");
        }

        @Override
        public Optional<Reservation> reservation(long id) {
            return findReservation(id, "");
        }

        private Optional<Reservation> findReservation(long id, String suffix) {
            return sql(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT " + RESERVATION_COLUMNS + " FROM reservations WHERE id = ?" + suffix)) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() ? Optional.of(reservation(rs)) : Optional.empty();
                    }
                }
            });
        }

        private static Reservation reservation(ResultSet rs) throws SQLException {
            return new Reservation(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getDate(6).toLocalDate(), rs.getDate(7).toLocalDate(), rs.getInt(8),
                    RatePlan.valueOf(rs.getString(9)), rs.getInt(10), rs.getBigDecimal(11), rs.getBigDecimal(12),
                    rs.getBigDecimal(13), ReservationStatus.valueOf(rs.getString(14)), rs.getBigDecimal(15),
                    rs.getBigDecimal(16));
        }

        @Override
        public void update(Reservation r) {
            sql(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
                    ps.setString(1, r.status().name());
                    ps.setBigDecimal(2, r.amountPaid());
                    ps.setBigDecimal(3, r.refund());
                    ps.setLong(4, r.id());
                    ps.executeUpdate();
                }
                return null;
            });
        }

        private List<Reservation> query(String where, String order, SqlBinder binder) {
            return sql(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT " + RESERVATION_COLUMNS + " FROM reservations WHERE " + where + " ORDER BY " + order)) {
                    binder.bind(ps);
                    try (ResultSet rs = ps.executeQuery()) {
                        List<Reservation> list = new ArrayList<>();
                        while (rs.next()) list.add(reservation(rs));
                        return list;
                    }
                }
            });
        }

        @Override
        public List<Reservation> startingBetween(LocalDate from, LocalDate to) {
            return query("check_in >= ? AND check_in <= ?", "check_in, id", ps -> {
                ps.setDate(1, Date.valueOf(from));
                ps.setDate(2, Date.valueOf(to));
            });
        }

        @Override
        public List<Reservation> occupying(LocalDate night) {
            return query("check_in <= ? AND check_out > ?", "room_number, id", ps -> {
                ps.setDate(1, Date.valueOf(night));
                ps.setDate(2, Date.valueOf(night));
            });
        }

        @Override
        public Message addMessage(String channel, String to, String subject, String body) {
            return sql(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO messages(channel, recipient, subject, body) VALUES (?,?,?,?) RETURNING id")) {
                    ps.setString(1, channel);
                    ps.setString(2, to);
                    ps.setString(3, subject);
                    ps.setString(4, body);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        return new Message(rs.getLong(1), channel, to, subject, body);
                    }
                }
            });
        }

        @Override
        public List<Message> messages() {
            return sql(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
                     ResultSet rs = ps.executeQuery()) {
                    List<Message> list = new ArrayList<>();
                    while (rs.next()) {
                        list.add(new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                                rs.getString(5)));
                    }
                    return list;
                }
            });
        }
    }

    @FunctionalInterface
    private interface SqlWork<T> { T run(Connection c) throws SQLException; }

    @FunctionalInterface
    private interface SqlBinder { void bind(PreparedStatement ps) throws SQLException; }
}
