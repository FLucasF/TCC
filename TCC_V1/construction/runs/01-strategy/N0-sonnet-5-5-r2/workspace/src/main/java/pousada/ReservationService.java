package pousada;

import pousada.plan.BookingContext;
import pousada.plan.Quote;
import pousada.plan.RatePlan;
import pousada.plan.RatePlans;

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

public class ReservationService {
    public static final String CONFIRMATION_SUBJECT = "Reserva confirmada - Pousada Maré Alta";

    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, RatePlan plan,
                                 String companyCode) {}

    public record Message(long id, String channel, String to, String subject, String body) {}

    private interface Work<T> { T run(Connection c) throws SQLException; }

    private static final String SELECT_RESERVATION = """
            SELECT r.*, ro.type AS room_type FROM reservations r JOIN rooms ro ON ro.number = r.room_number
            """;

    private final DataSource ds;

    public ReservationService(DataSource ds) {
        this.ds = ds;
    }

    private <T> T tx(Work<T> work) {
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
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

    // ---------- consultas ----------

    public List<Room> rooms() {
        return tx(c -> {
            List<Room> list = new ArrayList<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM rooms ORDER BY number")) {
                while (rs.next()) list.add(room(rs));
            }
            return list;
        });
    }

    public List<Message> messages() {
        return tx(c -> {
            List<Message> list = new ArrayList<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM messages ORDER BY id")) {
                while (rs.next()) {
                    list.add(new Message(rs.getLong("id"), rs.getString("channel"), rs.getString("recipient"),
                            rs.getString("subject"), rs.getString("body")));
                }
            }
            return list;
        });
    }

    public Reservation get(long id) {
        return tx(c -> load(c, id, false));
    }

    /** Segunda via: só para reserva paga. */
    public Reservation document(long id) {
        Reservation r = get(id);
        if (!r.isPaid()) throw ApiException.notAllowed("Só reservas pagas têm comprovante");
        return r;
    }

    // ---------- criação ----------

    public Reservation create(NewReservation n) {
        return tx(c -> {
            Room room;
            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM rooms WHERE number = ? FOR UPDATE")) {
                ps.setInt(1, n.roomNumber());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + n.roomNumber() + " não existe");
                    room = room(rs);
                }
            }
            BookingContext ctx = new BookingContext(room, n.checkIn(), n.checkOut(), n.guests(), n.companyCode());
            n.plan().validate(ctx);
            if (n.guests() > room.capacity()) {
                throw new ApiException(422, "CAPACITY_EXCEEDED",
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " pessoas");
            }
            // a linha do quarto está travada: duas reservas simultâneas do mesmo quarto se enfileiram aqui
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT 1 FROM reservations
                    WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ? LIMIT 1""")) {
                ps.setInt(1, room.number());
                ps.setDate(2, Date.valueOf(n.checkOut()));
                ps.setDate(3, Date.valueOf(n.checkIn()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites");
                }
            }
            Quote q = n.plan().quote(ctx);
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                        guests, rate_plan, company_code, nights, subtotal, discount, total, status)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,'PENDING') RETURNING id""")) {
                int i = 1;
                ps.setInt(i++, room.number());
                ps.setString(i++, n.guestName());
                ps.setString(i++, n.guestEmail());
                ps.setString(i++, n.guestCpf());
                ps.setDate(i++, Date.valueOf(n.checkIn()));
                ps.setDate(i++, Date.valueOf(n.checkOut()));
                ps.setInt(i++, n.guests());
                ps.setString(i++, n.plan().code());
                if (n.companyCode() != null && n.plan().code().equals("CORPORATE")) ps.setString(i++, n.companyCode());
                else ps.setNull(i++, Types.VARCHAR);
                ps.setInt(i++, ctx.nights());
                ps.setBigDecimal(i++, q.subtotal());
                ps.setBigDecimal(i++, q.discount());
                ps.setBigDecimal(i++, q.total());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return load(c, rs.getLong(1), false);
                }
            }
        });
    }

    // ---------- ciclo de vida ----------

    public Reservation confirm(long id) {
        return tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "confirmar", "PENDING");
            update(c, "UPDATE reservations SET status = 'CONFIRMED', amount_paid = total WHERE id = ?", id);
            Reservation confirmed = load(c, id, false);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO messages (channel, recipient, subject, body) VALUES ('EMAIL', ?, ?, ?)")) {
                ps.setString(1, confirmed.guestEmail());
                ps.setString(2, CONFIRMATION_SUBJECT);
                ps.setString(3, Receipt.confirmationEmailBody(confirmed));
                ps.executeUpdate();
            }
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "cancelar", "PENDING", "CONFIRMED");
            BigDecimal refund = Money.round(RatePlans.get(r.ratePlan()).refund(r.amountPaid()));
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE reservations SET status = 'CANCELLED', refund = ? WHERE id = ?")) {
                ps.setBigDecimal(1, refund);
                ps.setLong(2, id);
                ps.executeUpdate();
            }
            return load(c, id, false);
        });
    }

    public Reservation checkIn(long id) {
        return tx(c -> {
            requireStatus(load(c, id, true), "fazer check-in", "CONFIRMED");
            update(c, "UPDATE reservations SET status = 'CHECKED_IN' WHERE id = ?", id);
            return load(c, id, false);
        });
    }

    public Reservation checkOut(long id) {
        return tx(c -> {
            requireStatus(load(c, id, true), "fazer check-out", "CHECKED_IN");
            update(c, "UPDATE reservations SET status = 'CHECKED_OUT' WHERE id = ?", id);
            return load(c, id, false);
        });
    }

    // ---------- relatórios ----------

    public String occupancyCsv(LocalDate night) {
        return tx(c -> {
            StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
            int occupied = 0;
            try (PreparedStatement ps = c.prepareStatement(SELECT_RESERVATION + """
                     WHERE r.status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') AND r.check_in <= ? AND r.check_out > ?
                     ORDER BY r.room_number, r.id""")) {
                ps.setDate(1, Date.valueOf(night));
                ps.setDate(2, Date.valueOf(night));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Reservation r = reservation(rs);
                        occupied++;
                        sb.append(r.roomNumber()).append(',').append(r.roomType()).append(',').append(r.id()).append(',')
                          .append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
                    }
                }
            }
            int total;
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT count(*) FROM rooms")) {
                rs.next();
                total = rs.getInt(1);
            }
            return sb.append("total,").append(occupied).append('/').append(total).append('\n').toString();
        });
    }

    public String revenueCsv(java.time.YearMonth month) {
        return tx(c -> {
            StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal sum = Money.ZERO;
            try (PreparedStatement ps = c.prepareStatement(SELECT_RESERVATION + """
                     WHERE r.check_in >= ? AND r.check_in <= ? AND r.amount_paid - COALESCE(r.refund, 0) > 0
                     ORDER BY r.check_in, r.id""")) {
                ps.setDate(1, Date.valueOf(month.atDay(1)));
                ps.setDate(2, Date.valueOf(month.atEndOfMonth()));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Reservation r = reservation(rs);
                        BigDecimal kept = r.amountPaid().subtract(r.refund() == null ? BigDecimal.ZERO : r.refund());
                        sum = sum.add(kept);
                        sb.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                          .append(r.checkOut()).append(',').append(r.status()).append(',').append(Money.plain(kept)).append('\n');
                    }
                }
            }
            return sb.append("total,").append(Money.plain(sum)).append('\n').toString();
        });
    }

    // ---------- internos ----------

    private static void requireStatus(Reservation r, String action, String... allowed) {
        for (String s : allowed) if (r.status().equals(s)) return;
        throw ApiException.notAllowed("Não é possível " + action + " uma reserva com status " + r.status());
    }

    private static void update(Connection c, String sql, long id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private static Reservation load(Connection c, long id, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                SELECT_RESERVATION + "WHERE r.id = ?" + (lock ? " FOR UPDATE OF r" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe");
                return reservation(rs);
            }
        }
    }

    private static Room room(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), RoomType.valueOf(rs.getString("type")), rs.getInt("capacity"),
                rs.getBigDecimal("nightly_rate"));
    }

    private static Reservation reservation(ResultSet rs) throws SQLException {
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), RoomType.valueOf(rs.getString("room_type")),
                rs.getString("guest_name"), rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(), rs.getInt("guests"),
                rs.getString("rate_plan"), rs.getInt("nights"), rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"), rs.getBigDecimal("total"), rs.getString("status"),
                rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }
}
