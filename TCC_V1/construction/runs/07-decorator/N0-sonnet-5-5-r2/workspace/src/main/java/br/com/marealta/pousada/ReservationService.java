package br.com.marealta.pousada;

import br.com.marealta.pousada.Domain.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {
    private static final String RES_COLS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final Database db;
    private final MessageDispatcher dispatcher;

    public ReservationService(Database db, MessageDispatcher dispatcher) {
        this.db = db;
        this.dispatcher = dispatcher;
    }

    // ---------- consultas ----------

    public List<Room> rooms() {
        return db.tx(c -> {
            List<Room> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(room(rs));
            }
            return out;
        });
    }

    public Reservation get(long id) {
        return db.tx(c -> load(c, id, false));
    }

    public List<Message> messages() {
        return db.tx(JdbcMessageStore::listAll);
    }

    public Receipt document(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, false);
            if (!r.status().isPaid()) {
                throw ApiException.notAllowed("A segunda via só existe para reserva paga.");
            }
            return receipt(c, r);
        });
    }

    // ---------- operações ----------

    public Reservation create(NewReservation n) {
        return db.tx(c -> {
            // Trava o quarto: reservas concorrentes do mesmo quarto são atendidas uma por vez.
            Room room = lockRoom(c, n.roomNumber());
            if (n.guests() > room.capacity()) {
                throw new ApiException(422, "CAPACITY_EXCEEDED",
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " hóspedes.");
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                            + "AND check_in < ? AND check_out > ? LIMIT 1")) {
                ps.setInt(1, room.number());
                ps.setDate(2, Date.valueOf(n.checkOut()));
                ps.setDate(3, Date.valueOf(n.checkIn()));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        throw new ApiException(409, "ROOM_UNAVAILABLE",
                                "O quarto " + room.number() + " já está reservado em alguma das noites pedidas.");
                    }
                }
            }
            Pricing.Quote q = Pricing.quote(room.nightlyRate(), n.checkIn(), n.checkOut(), n.ratePlan());
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO reservations(room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, "
                            + "rate_plan, nights, subtotal, discount, total, status, amount_paid) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,'PENDING',0) RETURNING id")) {
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

    public Reservation confirm(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "confirmar", Status.PENDING);
            update(c, id, "status = 'CONFIRMED', amount_paid = total");
            Reservation confirmed = load(c, id, false);
            Receipt receipt = receipt(c, confirmed);
            dispatcher.dispatch(new JdbcMessageStore(c), new MessageDispatcher.Draft(
                    "EMAIL", confirmed.guestEmail(), Receipt.CONFIRMATION_SUBJECT, receipt.confirmationEmailBody()));
            return confirmed;
        });
    }

    public Reservation checkIn(long id) {
        return db.tx(c -> {
            requireStatus(load(c, id, true), "fazer check-in de", Status.CONFIRMED);
            update(c, id, "status = 'CHECKED_IN'");
            return load(c, id, false);
        });
    }

    public Receipt checkOut(long id) {
        return db.tx(c -> {
            requireStatus(load(c, id, true), "fazer check-out de", Status.CHECKED_IN);
            update(c, id, "status = 'CHECKED_OUT'");
            return receipt(c, load(c, id, false));
        });
    }

    public Reservation cancel(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "cancelar", Status.PENDING, Status.CONFIRMED);
            // Reembolso integral só no plano normal; reserva pendente não tem nada pago (amount_paid = 0).
            String refund = r.ratePlan() == RatePlan.STANDARD ? "amount_paid" : "0";
            update(c, id, "status = 'CANCELLED', refund = " + refund);
            return load(c, id, false);
        });
    }

    // ---------- relatórios ----------

    public String occupancyCsv(LocalDate date) {
        return db.tx(c -> {
            StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
            int occupied = 0;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT r.room_number, rm.type, r.id, r.check_in, r.check_out FROM reservations r "
                            + "JOIN rooms rm ON rm.number = r.room_number "
                            + "WHERE r.status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') AND r.check_in <= ? AND r.check_out > ? "
                            + "ORDER BY r.room_number, r.id")) {
                ps.setDate(1, Date.valueOf(date));
                ps.setDate(2, Date.valueOf(date));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        occupied++;
                        sb.append(rs.getInt(1)).append(',').append(rs.getString(2)).append(',').append(rs.getLong(3))
                                .append(',').append(rs.getDate(4).toLocalDate()).append(',')
                                .append(rs.getDate(5).toLocalDate()).append('\n');
                    }
                }
            }
            int total;
            try (PreparedStatement ps = c.prepareStatement("SELECT count(*) FROM rooms");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                total = rs.getInt(1);
            }
            return sb.append("total,").append(occupied).append('/').append(total).append('\n').toString();
        });
    }

    public String revenueCsv(YearMonth month) {
        return db.tx(c -> {
            StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal sum = Pricing.money(BigDecimal.ZERO);
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, room_number, check_in, check_out, status, amount_paid - COALESCE(refund, 0) AS amount "
                            + "FROM reservations WHERE check_in >= ? AND check_in < ? AND amount_paid - COALESCE(refund, 0) > 0 "
                            + "ORDER BY check_in, id")) {
                ps.setDate(1, Date.valueOf(month.atDay(1)));
                ps.setDate(2, Date.valueOf(month.plusMonths(1).atDay(1)));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal amount = rs.getBigDecimal(6);
                        sum = sum.add(amount);
                        sb.append(rs.getLong(1)).append(',').append(rs.getInt(2)).append(',')
                                .append(rs.getDate(3).toLocalDate()).append(',').append(rs.getDate(4).toLocalDate())
                                .append(',').append(rs.getString(5)).append(',').append(amount.toPlainString()).append('\n');
                    }
                }
            }
            return sb.append("total,").append(sum.toPlainString()).append('\n').toString();
        });
    }

    // ---------- internos ----------

    private static void requireStatus(Reservation r, String action, Status... allowed) {
        for (Status s : allowed) if (r.status() == s) return;
        throw ApiException.notAllowed("Não é possível " + action + " uma reserva com situação " + r.status() + ".");
    }

    private static Room room(ResultSet rs) throws SQLException {
        return new Room(rs.getInt(1), rs.getString(2), rs.getInt(3), rs.getBigDecimal(4));
    }

    private static Room lockRoom(Connection c, int number) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ? FOR UPDATE")) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + number + " não existe.");
                return room(rs);
            }
        }
    }

    private static Reservation load(Connection c, long id, boolean forUpdate) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + RES_COLS + " FROM reservations WHERE id = ?" + (forUpdate ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe.");
                }
                return new Reservation(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getDate(6).toLocalDate(), rs.getDate(7).toLocalDate(), rs.getInt(8),
                        RatePlan.valueOf(rs.getString(9)), rs.getInt(10), rs.getBigDecimal(11), rs.getBigDecimal(12),
                        rs.getBigDecimal(13), Status.valueOf(rs.getString(14)), rs.getBigDecimal(15),
                        rs.getBigDecimal(16));
            }
        }
    }

    private static void update(Connection c, long id, String setClause) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE reservations SET " + setClause + " WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private static Receipt receipt(Connection c, Reservation r) throws SQLException {
        String type;
        try (PreparedStatement ps = c.prepareStatement("SELECT type FROM rooms WHERE number = ?")) {
            ps.setInt(1, r.roomNumber());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                type = rs.getString(1);
            }
        }
        return new Receipt(r.id(), r.guestName(), r.guestCpf(), r.roomNumber(), type, r.checkIn(), r.checkOut(),
                r.nights(), r.subtotal(), r.discount(), r.total());
    }
}
