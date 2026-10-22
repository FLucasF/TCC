package br.com.maretalta.store;

import br.com.maretalta.domain.Message;
import br.com.maretalta.domain.RatePlan;
import br.com.maretalta.domain.Reservation;
import br.com.maretalta.domain.Status;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class PostgresStore implements Store, AutoCloseable {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final HikariDataSource ds;

    public PostgresStore(String url, String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setInitializationFailTimeout(-1); // o banco pode demorar a subir; tentamos nós mesmos
        this.ds = new HikariDataSource(cfg);
    }

    /** Cria o que falta no banco, esperando-o ficar disponível por até {@code waitMillis}. */
    public void initSchema(long waitMillis) {
        long deadline = System.currentTimeMillis() + waitMillis;
        while (true) {
            try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
                c.setAutoCommit(false);
                st.execute("SELECT pg_advisory_xact_lock(7070)"); // várias instâncias subindo juntas
                st.execute("""
                        CREATE TABLE IF NOT EXISTS reservations (
                            id BIGSERIAL PRIMARY KEY,
                            room_number INT NOT NULL,
                            guest_name TEXT NOT NULL,
                            guest_email TEXT NOT NULL,
                            guest_cpf TEXT NOT NULL,
                            check_in DATE NOT NULL,
                            check_out DATE NOT NULL,
                            guests INT NOT NULL,
                            rate_plan TEXT NOT NULL,
                            nights INT NOT NULL,
                            subtotal NUMERIC(12,2) NOT NULL,
                            discount NUMERIC(12,2) NOT NULL,
                            total NUMERIC(12,2) NOT NULL,
                            status TEXT NOT NULL,
                            amount_paid NUMERIC(12,2) NOT NULL,
                            refund NUMERIC(12,2)
                        )""");
                st.execute("CREATE INDEX IF NOT EXISTS reservations_room_idx ON reservations (room_number, check_in)");
                st.execute("CREATE INDEX IF NOT EXISTS reservations_check_in_idx ON reservations (check_in, id)");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS messages (
                            id BIGSERIAL PRIMARY KEY,
                            channel TEXT NOT NULL,
                            recipient TEXT NOT NULL,
                            subject TEXT NOT NULL,
                            body TEXT NOT NULL
                        )""");
                c.commit();
                return;
            } catch (SQLException e) {
                if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Banco indisponível", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ie);
                }
            }
        }
    }

    @Override
    public <T> T inTransaction(Function<Tx, T> work) {
        try (Connection c = ds.getConnection()) {
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
        ds.close();
    }

    private static final class PgTx implements Tx {
        private final Connection c;

        PgTx(Connection c) { this.c = c; }

        private <T> T sql(SqlWork<T> w) {
            try {
                return w.run();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }

        private interface SqlWork<T> { T run() throws SQLException; }

        @Override
        public void lockRoom(int roomNumber) {
            sql(() -> {
                try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
                    ps.setLong(1, 1_000_000L + roomNumber);
                    ps.execute();
                }
                return null;
            });
        }

        @Override
        public boolean hasOverlap(int room, LocalDate in, LocalDate out) {
            return sql(() -> {
                try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM reservations WHERE room_number = ? "
                        + "AND status <> 'CANCELLED' AND check_in < ? AND ? < check_out LIMIT 1")) {
                    ps.setInt(1, room);
                    ps.setDate(2, Date.valueOf(out));
                    ps.setDate(3, Date.valueOf(in));
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next();
                    }
                }
            });
        }

        @Override
        public Reservation insert(Reservation d) {
            return sql(() -> {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO reservations (room_number, guest_name, "
                        + "guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, "
                        + "total, status, amount_paid, refund) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id")) {
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
                        return d.withId(rs.getLong(1));
                    }
                }
            });
        }

        @Override public Optional<Reservation> findForUpdate(long id) { return findById(id, " FOR UPDATE"); }

        @Override public Optional<Reservation> find(long id) { return findById(id, ""); }

        private Optional<Reservation> findById(long id, String suffix) {
            return sql(() -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT " + COLUMNS + " FROM reservations WHERE id = ?" + suffix)) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() ? Optional.of(read(rs)) : Optional.empty();
                    }
                }
            });
        }

        @Override
        public void update(Reservation r) {
            sql(() -> {
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

        @Override
        public List<Reservation> startingIn(YearMonth month) {
            return query("WHERE check_in >= ? AND check_in < ? ORDER BY check_in, id",
                    month.atDay(1), month.plusMonths(1).atDay(1));
        }

        @Override
        public List<Reservation> paidOccupying(LocalDate night) {
            return query("WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') AND check_in <= ? AND ? < check_out "
                    + "ORDER BY room_number, id", night, night);
        }

        private List<Reservation> query(String tail, LocalDate... dates) {
            return sql(() -> {
                try (PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM reservations " + tail)) {
                    for (int i = 0; i < dates.length; i++) ps.setDate(i + 1, Date.valueOf(dates[i]));
                    try (ResultSet rs = ps.executeQuery()) {
                        List<Reservation> out = new ArrayList<>();
                        while (rs.next()) out.add(read(rs));
                        return out;
                    }
                }
            });
        }

        @Override
        public Message addMessage(String channel, String to, String subject, String body) {
            return sql(() -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO messages (channel, recipient, subject, body) VALUES (?,?,?,?) RETURNING id")) {
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
            return sql(() -> {
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT id, channel, recipient, subject, body FROM messages ORDER BY id")) {
                    List<Message> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
                    }
                    return out;
                }
            });
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
                    RatePlan.valueOf(rs.getString("rate_plan")),
                    rs.getInt("nights"),
                    rs.getBigDecimal("subtotal"),
                    rs.getBigDecimal("discount"),
                    rs.getBigDecimal("total"),
                    Status.valueOf(rs.getString("status")),
                    rs.getBigDecimal("amount_paid"),
                    rs.getBigDecimal("refund"));
        }
    }
}
