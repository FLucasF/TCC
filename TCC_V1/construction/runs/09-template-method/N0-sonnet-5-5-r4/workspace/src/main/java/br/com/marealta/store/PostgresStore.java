package br.com.marealta.store;

import br.com.marealta.domain.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class PostgresStore implements Store, AutoCloseable {
    private static final String RESERVATION_COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, "
            + "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, "
            + "amount_paid, refund";

    private final HikariDataSource dataSource;

    public PostgresStore(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setInitializationFailTimeout(-1);
        this.dataSource = new HikariDataSource(config);
    }

    /** Creates the schema and the rooms, waiting for the database to accept connections. */
    public void initialize() {
        SQLException last = null;
        for (int attempt = 0; attempt < 60; attempt++) {
            try (Connection c = dataSource.getConnection()) {
                createSchema(c);
                return;
            } catch (SQLException e) {
                last = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new IllegalStateException("Database unavailable", last);
    }

    private void createSchema(Connection c) throws SQLException {
        c.setAutoCommit(false);
        try (Statement s = c.createStatement()) {
            s.execute("SELECT pg_advisory_xact_lock(7070)");
            s.execute("CREATE TABLE IF NOT EXISTS rooms ("
                    + "number INT PRIMARY KEY, type VARCHAR(20) NOT NULL, capacity INT NOT NULL, "
                    + "nightly_rate NUMERIC(12,2) NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS reservations ("
                    + "id BIGSERIAL PRIMARY KEY, room_number INT NOT NULL REFERENCES rooms(number), "
                    + "guest_name TEXT NOT NULL, guest_email TEXT NOT NULL, guest_cpf TEXT NOT NULL, "
                    + "check_in DATE NOT NULL, check_out DATE NOT NULL, guests INT NOT NULL, "
                    + "rate_plan VARCHAR(20) NOT NULL, nights INT NOT NULL, "
                    + "subtotal NUMERIC(12,2) NOT NULL, discount NUMERIC(12,2) NOT NULL, "
                    + "total NUMERIC(12,2) NOT NULL, status VARCHAR(20) NOT NULL, "
                    + "amount_paid NUMERIC(12,2) NOT NULL, refund NUMERIC(12,2))");
            s.execute("CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations(room_number, check_in)");
            s.execute("CREATE INDEX IF NOT EXISTS reservations_check_in_idx ON reservations(check_in)");
            s.execute("CREATE TABLE IF NOT EXISTS messages ("
                    + "id BIGSERIAL PRIMARY KEY, channel VARCHAR(20) NOT NULL, recipient TEXT NOT NULL, "
                    + "subject TEXT NOT NULL, body TEXT NOT NULL)");
            s.execute("INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES "
                    + "(101, 'STANDARD', 2, 249.90), (102, 'STANDARD', 2, 249.90), "
                    + "(201, 'DELUXE', 3, 389.90), (202, 'DELUXE', 3, 389.90), "
                    + "(301, 'SUITE', 4, 579.90) ON CONFLICT (number) DO NOTHING");
        }
        c.commit();
    }

    @Override
    public <T> T inTransaction(Function<Tx, T> work) {
        try (Connection c = dataSource.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.apply(new PgTx(c));
                c.commit();
                return result;
            } catch (Throwable t) {
                c.rollback();
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
        private final Connection c;

        PgTx(Connection c) {
            this.c = c;
        }

        @Override
        public List<Room> rooms() {
            return query("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number",
                    ps -> {
                    }, PostgresStore::room);
        }

        @Override
        public Optional<Room> room(int number) {
            return query("SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?",
                    ps -> ps.setInt(1, number), PostgresStore::room).stream().findFirst();
        }

        @Override
        public void lockRoom(int number) {
            query("SELECT number FROM rooms WHERE number = ? FOR UPDATE", ps -> ps.setInt(1, number),
                    rs -> rs.getInt(1));
        }

        @Override
        public boolean roomBooked(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
            return !query("SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                            + "AND check_in < ? AND ? < check_out LIMIT 1",
                    ps -> {
                        ps.setInt(1, roomNumber);
                        ps.setObject(2, checkOut);
                        ps.setObject(3, checkIn);
                    }, rs -> 1).isEmpty();
        }

        @Override
        public Reservation insert(Reservation r) {
            long id = query("INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, "
                            + "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, "
                            + "status, amount_paid, refund) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id",
                    ps -> {
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
                    }, rs -> rs.getLong(1)).get(0);
            return new Reservation(id, r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(), r.checkIn(),
                    r.checkOut(), r.guests(), r.ratePlan(), r.nights(), r.subtotal(), r.discount(), r.total(),
                    r.status(), r.amountPaid(), r.refund());
        }

        @Override
        public void update(Reservation r) {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
                ps.setString(1, r.status().name());
                ps.setBigDecimal(2, r.amountPaid());
                ps.setBigDecimal(3, r.refund());
                ps.setLong(4, r.id());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Optional<Reservation> reservation(long id) {
            return query("SELECT " + RESERVATION_COLUMNS + " FROM reservations WHERE id = ?",
                    ps -> ps.setLong(1, id), PostgresStore::reservation).stream().findFirst();
        }

        @Override
        public Optional<Reservation> reservationForUpdate(long id) {
            return query("SELECT " + RESERVATION_COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE",
                    ps -> ps.setLong(1, id), PostgresStore::reservation).stream().findFirst();
        }

        @Override
        public List<Reservation> reservationsCheckingInBetween(LocalDate from, LocalDate toExclusive) {
            return query("SELECT " + RESERVATION_COLUMNS + " FROM reservations "
                            + "WHERE check_in >= ? AND check_in < ? ORDER BY check_in, id",
                    ps -> {
                        ps.setObject(1, from);
                        ps.setObject(2, toExclusive);
                    }, PostgresStore::reservation);
        }

        @Override
        public List<Reservation> reservationsCoveringNight(LocalDate night) {
            return query("SELECT " + RESERVATION_COLUMNS + " FROM reservations "
                            + "WHERE check_in <= ? AND ? < check_out ORDER BY room_number, id",
                    ps -> {
                        ps.setObject(1, night);
                        ps.setObject(2, night);
                    }, PostgresStore::reservation);
        }

        @Override
        public Message saveMessage(Message m) {
            long id = query("INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?) RETURNING id",
                    ps -> {
                        ps.setString(1, m.channel().name());
                        ps.setString(2, m.to());
                        ps.setString(3, m.subject());
                        ps.setString(4, m.body());
                    }, rs -> rs.getLong(1)).get(0);
            return new Message(id, m.channel(), m.to(), m.subject(), m.body());
        }

        @Override
        public List<Message> messages() {
            return query("SELECT id, channel, recipient, subject, body FROM messages ORDER BY id",
                    ps -> {
                    }, rs -> new Message(rs.getLong(1), Channel.valueOf(rs.getString(2)), rs.getString(3),
                            rs.getString(4), rs.getString(5)));
        }

        private <R> List<R> query(String sql, Binder binder, Mapper<R> mapper) {
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                binder.bind(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    List<R> out = new ArrayList<>();
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
    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    private interface Mapper<R> {
        R map(ResultSet rs) throws SQLException;
    }

    private static Room room(ResultSet rs) throws SQLException {
        return new Room(rs.getInt(1), RoomType.valueOf(rs.getString(2)), rs.getInt(3), rs.getBigDecimal(4));
    }

    private static Reservation reservation(ResultSet rs) throws SQLException {
        BigDecimal refund = rs.getBigDecimal(16);
        return new Reservation(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getObject(6, LocalDate.class), rs.getObject(7, LocalDate.class), rs.getInt(8),
                RatePlan.valueOf(rs.getString(9)), rs.getInt(10), rs.getBigDecimal(11), rs.getBigDecimal(12),
                rs.getBigDecimal(13), ReservationStatus.valueOf(rs.getString(14)), rs.getBigDecimal(15), refund);
    }
}
