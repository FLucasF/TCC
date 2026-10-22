package br.com.marealta.store;

import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import br.com.marealta.event.Message;
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

public class JdbcStore implements Store, AutoCloseable {
    private static final String RES_COLS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final HikariDataSource ds;

    public JdbcStore(String url, String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(10);
        cfg.setInitializationFailTimeout(60_000); // o banco pode demorar a subir
        this.ds = new HikariDataSource(cfg);
        migrate();
    }

    private void migrate() {
        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            // Evita corrida se mais de uma instância subir junto num banco vazio.
            st.execute("select pg_advisory_lock(727001)");
            try {
                st.execute("create table if not exists rooms (number int primary key, type text not null, "
                        + "capacity int not null, nightly_rate numeric(12,2) not null)");
                st.execute("create table if not exists reservations (id bigserial primary key, "
                        + "room_number int not null references rooms(number), guest_name text not null, "
                        + "guest_email text not null, guest_cpf text not null, check_in date not null, "
                        + "check_out date not null, guests int not null, rate_plan text not null, nights int not null, "
                        + "subtotal numeric(12,2) not null, discount numeric(12,2) not null, total numeric(12,2) not null, "
                        + "status text not null, amount_paid numeric(12,2) not null, refund numeric(12,2))");
                st.execute("create index if not exists reservations_room_idx on reservations (room_number, check_in)");
                st.execute("create table if not exists messages (id bigserial primary key, channel text not null, "
                        + "recipient text not null, subject text not null, body text not null)");
                for (Room r : Room.ALL) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "insert into rooms (number, type, capacity, nightly_rate) values (?,?,?,?) "
                                    + "on conflict (number) do nothing")) {
                        ps.setInt(1, r.number());
                        ps.setString(2, r.type().name());
                        ps.setInt(3, r.capacity());
                        ps.setBigDecimal(4, r.nightlyRate());
                        ps.executeUpdate();
                    }
                }
            } finally {
                st.execute("select pg_advisory_unlock(727001)");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Falha ao preparar o banco", e);
        }
    }

    @Override
    public <T> T tx(Function<Tx, T> work) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.apply(new JdbcTx(c));
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

    private static final class JdbcTx implements Tx {
        private final Connection c;

        JdbcTx(Connection c) { this.c = c; }

        private <T> T sql(SqlFn<T> f) {
            try {
                return f.apply(c);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public List<Room> rooms() {
            return sql(c -> {
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("select number, type, capacity, nightly_rate from rooms order by number")) {
                    List<Room> out = new ArrayList<>();
                    while (rs.next()) out.add(room(rs));
                    return out;
                }
            });
        }

        @Override
        public Optional<Room> room(int number) {
            return sql(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "select number, type, capacity, nightly_rate from rooms where number = ?")) {
                    ps.setInt(1, number);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() ? Optional.of(room(rs)) : Optional.<Room>empty();
                    }
                }
            });
        }

        @Override
        public void lockRoom(int number) {
            sql(c -> {
                try (PreparedStatement ps = c.prepareStatement("select 1 from rooms where number = ? for update")) {
                    ps.setInt(1, number);
                    ps.executeQuery().close();
                    return null;
                }
            });
        }

        @Override
        public boolean hasOverlap(int roomNumber, LocalDate in, LocalDate out) {
            return sql(c -> {
                try (PreparedStatement ps = c.prepareStatement("select 1 from reservations where room_number = ? "
                        + "and status <> 'CANCELLED' and check_in < ? and check_out > ? limit 1")) {
                    ps.setInt(1, roomNumber);
                    ps.setDate(2, Date.valueOf(out));
                    ps.setDate(3, Date.valueOf(in));
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next();
                    }
                }
            });
        }

        @Override
        public Reservation insert(Reservation r) {
            return sql(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "insert into reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                                + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                                + "values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) returning id")) {
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
                    ps.setBigDecimal(15, r.refund());
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        long id = rs.getLong(1);
                        return new Reservation(id, r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(),
                                r.checkIn(), r.checkOut(), r.guests(), r.ratePlan(), r.nights(), r.subtotal(),
                                r.discount(), r.total(), r.status(), r.amountPaid(), r.refund());
                    }
                }
            });
        }

        @Override
        public Optional<Reservation> find(long id) { return findBy(id, ""); }

        @Override
        public Optional<Reservation> findForUpdate(long id) { return findBy(id, " for update"); }

        private Optional<Reservation> findBy(long id, String suffix) {
            return sql(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "select " + RES_COLS + " from reservations where id = ?" + suffix)) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        return rs.next() ? Optional.of(reservation(rs)) : Optional.<Reservation>empty();
                    }
                }
            });
        }

        @Override
        public void update(Reservation r) {
            sql(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "update reservations set status = ?, amount_paid = ?, refund = ? where id = ?")) {
                    ps.setString(1, r.status().name());
                    ps.setBigDecimal(2, r.amountPaid());
                    ps.setBigDecimal(3, r.refund());
                    ps.setLong(4, r.id());
                    ps.executeUpdate();
                    return null;
                }
            });
        }

        @Override
        public List<Reservation> all() {
            return sql(c -> {
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("select " + RES_COLS + " from reservations order by id")) {
                    List<Reservation> out = new ArrayList<>();
                    while (rs.next()) out.add(reservation(rs));
                    return out;
                }
            });
        }

        @Override
        public void addMessage(Message m) {
            sql(c -> {
                try (PreparedStatement ps = c.prepareStatement(
                        "insert into messages (channel, recipient, subject, body) values (?,?,?,?)")) {
                    ps.setString(1, m.channel().name());
                    ps.setString(2, m.to());
                    ps.setString(3, m.subject());
                    ps.setString(4, m.body());
                    ps.executeUpdate();
                    return null;
                }
            });
        }

        @Override
        public List<Message> messages() {
            return sql(c -> {
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("select id, channel, recipient, subject, body from messages order by id")) {
                    List<Message> out = new ArrayList<>();
                    while (rs.next()) {
                        out.add(new Message(rs.getLong(1), Message.Channel.valueOf(rs.getString(2)),
                                rs.getString(3), rs.getString(4), rs.getString(5)));
                    }
                    return out;
                }
            });
        }

        private static Room room(ResultSet rs) throws SQLException {
            return new Room(rs.getInt(1), RoomType.valueOf(rs.getString(2)), rs.getInt(3), rs.getBigDecimal(4));
        }

        private static Reservation reservation(ResultSet rs) throws SQLException {
            return new Reservation(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
                    rs.getDate(6).toLocalDate(), rs.getDate(7).toLocalDate(), rs.getInt(8),
                    RatePlan.valueOf(rs.getString(9)), rs.getInt(10), rs.getBigDecimal(11), rs.getBigDecimal(12),
                    rs.getBigDecimal(13), ReservationStatus.valueOf(rs.getString(14)), rs.getBigDecimal(15),
                    rs.getBigDecimal(16));
        }
    }

    @FunctionalInterface
    private interface SqlFn<T> {
        T apply(Connection c) throws SQLException;
    }
}
