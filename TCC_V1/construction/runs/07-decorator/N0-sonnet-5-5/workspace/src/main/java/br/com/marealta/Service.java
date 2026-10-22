package br.com.marealta;

import br.com.marealta.Domain.RatePlan;
import br.com.marealta.Domain.Reservation;
import br.com.marealta.Domain.Room;
import br.com.marealta.Domain.RoomType;
import br.com.marealta.Domain.Status;
import br.com.marealta.Messaging.Draft;
import br.com.marealta.Messaging.Outgoing;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** Regras de negócio e acesso ao banco. */
public final class Service {
    private final Database db;
    private final Clock clock;
    private final Config config;
    private final Messaging.Pipeline pipeline;

    public record NewReservation(
            int roomNumber, String guestName, String guestEmail, String guestCpf,
            LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan) {}

    public record StoredMessage(
            long id, String channel, String to, String subject, String body, String status) {}

    public Service(Database db, Clock clock, Config config) {
        this.db = db;
        this.clock = clock;
        this.config = config;
        this.pipeline = new Messaging.Pipeline(config.notifications(), config.testMode());
    }

    private <T> T run(Database.Work<T> work) {
        try {
            return db.tx(work);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- quartos ----------

    public List<Room> rooms() {
        return run(c -> {
            List<Room> out = new ArrayList<>();
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number")) {
                while (rs.next()) out.add(room(rs));
            }
            return out;
        });
    }

    private static Room room(ResultSet rs) throws SQLException {
        return new Room(rs.getInt("number"), RoomType.valueOf(rs.getString("type")),
                rs.getInt("capacity"), rs.getBigDecimal("nightly_rate"));
    }

    private static Room findRoom(Connection c, int number, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + number + " não existe");
                }
                return room(rs);
            }
        }
    }

    // ---------- reservas ----------

    public Reservation create(NewReservation n) {
        return run(c -> {
            // o bloqueio da linha do quarto serializa reservas concorrentes do mesmo quarto
            Room room = findRoom(c, n.roomNumber(), true);
            if (n.guests() > room.capacity()) {
                throw new ApiException(422, "CAPACITY_EXCEEDED",
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " pessoas");
            }
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT 1 FROM reservations
                    WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ? LIMIT 1""")) {
                ps.setInt(1, room.number());
                ps.setDate(2, Date.valueOf(n.checkOut()));
                ps.setDate(3, Date.valueOf(n.checkIn()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        throw new ApiException(409, "ROOM_UNAVAILABLE", "Quarto já reservado em alguma das noites pedidas");
                    }
                }
            }
            Pricing.Quote q = Pricing.quote(room, n.checkIn(), n.checkOut(), n.ratePlan());
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                      guests, rate_plan, nights, subtotal, discount, total, status, amount_paid)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,'PENDING',0) RETURNING id""")) {
                ps.setInt(1, room.number());
                ps.setString(2, n.guestName());
                ps.setString(3, n.guestEmail());
                ps.setString(4, n.guestCpf());
                ps.setDate(5, Date.valueOf(n.checkIn()));
                ps.setDate(6, Date.valueOf(n.checkOut()));
                ps.setInt(7, n.guests());
                ps.setString(8, n.ratePlan().name());
                ps.setInt(9, q.nights());
                ps.setBigDecimal(10, q.subtotal());
                ps.setBigDecimal(11, q.discount());
                ps.setBigDecimal(12, q.total());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return load(c, rs.getLong(1), false);
                }
            }
        });
    }

    public Reservation get(long id) {
        return run(c -> load(c, id, false));
    }

    private static Reservation load(Connection c, long id, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM reservations WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe");
                }
                return reservation(rs);
            }
        }
    }

    private static Reservation reservation(ResultSet rs) throws SQLException {
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), rs.getInt("nights"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                Status.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }

    private static Reservation transition(Connection c, long id, Status from, Status to, String verb) throws SQLException {
        Reservation r = load(c, id, true);
        if (r.status() != from) {
            throw ApiException.notAllowed("Não é possível " + verb + " uma reserva com situação " + r.status());
        }
        try (PreparedStatement ps = c.prepareStatement("UPDATE reservations SET status = ? WHERE id = ?")) {
            ps.setString(1, to.name());
            ps.setLong(2, id);
            ps.executeUpdate();
        }
        return load(c, id, false);
    }

    public Reservation confirm(long id) {
        return run(c -> {
            Reservation r = load(c, id, true);
            if (r.status() != Status.PENDING) {
                throw ApiException.notAllowed("Não é possível confirmar uma reserva com situação " + r.status());
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE reservations SET status = 'CONFIRMED', amount_paid = total WHERE id = ?")) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }
            Reservation confirmed = load(c, id, false);
            String receipt = receipt(c, confirmed).render();
            String body = "Olá, " + confirmed.guestName() + "!\n\n"
                    + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                    + receipt + "\n\n"
                    + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                    + "Até breve!";
            send(c, new Draft(confirmed.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body));
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return run(c -> {
            Reservation r = load(c, id, true);
            if (r.status() != Status.PENDING && r.status() != Status.CONFIRMED) {
                throw ApiException.notAllowed("Não é possível cancelar uma reserva com situação " + r.status());
            }
            // plano normal devolve o que foi pago (nada, se estava pendente); não reembolsável não devolve
            BigDecimal refund = r.ratePlan() == RatePlan.STANDARD ? r.amountPaid() : BigDecimal.ZERO.setScale(2);
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
        return run(c -> transition(c, id, Status.CONFIRMED, Status.CHECKED_IN, "fazer check-in de"));
    }

    public Receipt checkOut(long id) {
        return run(c -> receipt(c, transition(c, id, Status.CHECKED_IN, Status.CHECKED_OUT, "fazer check-out de")));
    }

    public Receipt document(long id) {
        return run(c -> {
            Reservation r = load(c, id, false);
            if (!r.status().isPaid()) {
                throw ApiException.notAllowed("Só existe comprovante para reserva paga; situação atual: " + r.status());
            }
            return receipt(c, r);
        });
    }

    private static Receipt receipt(Connection c, Reservation r) throws SQLException {
        return Receipt.of(r, findRoom(c, r.roomNumber(), false).type());
    }

    // ---------- mensagens ----------

    private void send(Connection c, Draft draft) throws SQLException {
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(config.zone()));
        // serializa os envios para o mesmo hóspede, para o limite diário valer sob concorrência
        try (PreparedStatement ps = c.prepareStatement("SELECT pg_advisory_xact_lock(hashtext(?))")) {
            ps.setString(1, Messaging.key(draft.to()));
            ps.execute();
        }
        Messaging.Context ctx = new Messaging.Context(config.testMode(), now, addr -> {
            try {
                return countSentToday(c, addr, now);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });
        for (Outgoing m : pipeline.process(draft, ctx)) {
            try (PreparedStatement ps = c.prepareStatement("""
                    INSERT INTO messages (channel, recipient, subject, body, status, guest_address, copy, created_at)
                    VALUES (?,?,?,?,?,?,?,?)""")) {
                ps.setString(1, m.channel().name());
                ps.setString(2, m.to());
                ps.setString(3, m.subject());
                ps.setString(4, m.body());
                ps.setString(5, m.status().name());
                ps.setString(6, m.guestAddress());
                ps.setBoolean(7, m.copy());
                ps.setTimestamp(8, Timestamp.from(now.toInstant()));
                ps.executeUpdate();
            }
        }
    }

    private static int countSentToday(Connection c, String address, ZonedDateTime now) throws SQLException {
        ZonedDateTime start = now.toLocalDate().atStartOfDay(now.getZone());
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT count(*) FROM messages
                WHERE status = 'SENT' AND NOT copy AND lower(guest_address) = lower(?)
                  AND created_at >= ? AND created_at < ?""")) {
            ps.setString(1, address);
            ps.setTimestamp(2, Timestamp.from(start.toInstant()));
            ps.setTimestamp(3, Timestamp.from(start.plusDays(1).toInstant()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public List<StoredMessage> messages() {
        return run(c -> {
            List<StoredMessage> out = new ArrayList<>();
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT id, channel, recipient, subject, body, status FROM messages ORDER BY id")) {
                while (rs.next()) {
                    out.add(new StoredMessage(rs.getLong(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getString(5), rs.getString(6)));
                }
            }
            return out;
        });
    }

    // ---------- relatórios ----------

    public String occupancyCsv(LocalDate night) {
        return run(c -> {
            StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
            int occupied = 0;
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT r.room_number, m.type, r.id, r.check_in, r.check_out
                    FROM reservations r JOIN rooms m ON m.number = r.room_number
                    WHERE r.status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') AND r.check_in <= ? AND r.check_out > ?
                    ORDER BY r.room_number, r.id""")) {
                ps.setDate(1, Date.valueOf(night));
                ps.setDate(2, Date.valueOf(night));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        occupied++;
                        sb.append(rs.getInt(1)).append(',').append(rs.getString(2)).append(',')
                                .append(rs.getLong(3)).append(',').append(rs.getDate(4).toLocalDate()).append(',')
                                .append(rs.getDate(5).toLocalDate()).append('\n');
                    }
                }
            }
            int total;
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT count(*) FROM rooms")) {
                rs.next();
                total = rs.getInt(1);
            }
            return sb.append("total,").append(occupied).append('/').append(total).append('\n').toString();
        });
    }

    public String revenueCsv(YearMonth month) {
        return run(c -> {
            StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal sum = BigDecimal.ZERO.setScale(2);
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT id, room_number, check_in, check_out, status, amount_paid - COALESCE(refund, 0) AS amount
                    FROM reservations
                    WHERE check_in >= ? AND check_in < ? AND amount_paid - COALESCE(refund, 0) > 0
                    ORDER BY check_in, id""")) {
                ps.setDate(1, Date.valueOf(month.atDay(1)));
                ps.setDate(2, Date.valueOf(month.plusMonths(1).atDay(1)));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal amount = rs.getBigDecimal("amount").setScale(2);
                        sum = sum.add(amount);
                        sb.append(rs.getLong("id")).append(',').append(rs.getInt("room_number")).append(',')
                                .append(rs.getDate("check_in").toLocalDate()).append(',')
                                .append(rs.getDate("check_out").toLocalDate()).append(',')
                                .append(rs.getString("status")).append(',').append(amount.toPlainString()).append('\n');
                    }
                }
            }
            return sb.append("total,").append(sum.toPlainString()).append('\n').toString();
        });
    }
}
