package br.com.marealta;

import br.com.marealta.plan.RatePlan;
import br.com.marealta.plan.RatePlans;

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

    public record NewReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                 LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
                                 String companyCode) {}

    private static final String RES_COLS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund";

    private final Db db;

    public ReservationService(Db db) {
        this.db = db;
    }

    // ---------- consultas ----------

    public List<Room> rooms() {
        return db.tx(c -> {
            List<Room> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(room(rs));
                }
            }
            return out;
        });
    }

    public Reservation get(long id) {
        return db.tx(c -> load(c, id, false));
    }

    public Room roomOf(Reservation r) {
        return db.tx(c -> findRoom(c, r.roomNumber(), false));
    }

    public List<Message> messages() {
        return db.tx(c -> {
            List<Message> out = new ArrayList<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, channel, recipient, subject, body FROM messages ORDER BY id");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Message(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5)));
                }
            }
            return out;
        });
    }

    // ---------- operações ----------

    public Reservation create(NewReservation n) {
        return db.tx(c -> {
            Room room = findRoom(c, n.roomNumber(), true); // trava o quarto: serializa reservas concorrentes
            if (n.guests() > room.capacity()) {
                throw new ApiException(422, "CAPACITY_EXCEEDED",
                        "O quarto " + room.number() + " comporta no máximo " + room.capacity() + " pessoas.");
            }
            int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(n.checkIn(), n.checkOut());
            RatePlan plan = RatePlans.get(n.ratePlan());
            RatePlan.Booking booking = new RatePlan.Booking(room.type(), n.checkIn(), n.checkOut(), nights, n.companyCode());
            plan.validate(booking);
            RatePlan.Price price = Pricing.price(plan, booking, room.nightlyRate());

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

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                            + "guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,'PENDING',0) RETURNING id")) {
                ps.setInt(1, room.number());
                ps.setString(2, n.guestName());
                ps.setString(3, n.guestEmail());
                ps.setString(4, n.guestCpf());
                ps.setDate(5, Date.valueOf(n.checkIn()));
                ps.setDate(6, Date.valueOf(n.checkOut()));
                ps.setInt(7, n.guests());
                ps.setString(8, plan.code());
                ps.setString(9, plan.companyCodeToStore(booking));
                ps.setInt(10, nights);
                ps.setBigDecimal(11, price.subtotal());
                ps.setBigDecimal(12, price.discount());
                ps.setBigDecimal(13, price.total());
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
            update(c, id, Status.CONFIRMED, r.total(), null);
            Reservation confirmed = load(c, id, false);
            Room room = findRoom(c, r.roomNumber(), false);
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO messages (channel, recipient, subject, body) VALUES ('EMAIL', ?, ?, ?)")) {
                ps.setString(1, confirmed.guestEmail());
                ps.setString(2, "Reserva confirmada - Pousada Maré Alta");
                ps.setString(3, Receipt.confirmationEmail(confirmed, room));
                ps.executeUpdate();
            }
            return confirmed;
        });
    }

    public Reservation cancel(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "cancelar", Status.PENDING, Status.CONFIRMED);
            BigDecimal refund = RatePlans.get(r.ratePlan()).refund(r.amountPaid());
            update(c, id, Status.CANCELLED, r.amountPaid(), refund);
            return load(c, id, false);
        });
    }

    public Reservation checkIn(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "fazer check-in de", Status.CONFIRMED);
            update(c, id, Status.CHECKED_IN, r.amountPaid(), null);
            return load(c, id, false);
        });
    }

    public Reservation checkOut(long id) {
        return db.tx(c -> {
            Reservation r = load(c, id, true);
            requireStatus(r, "fazer check-out de", Status.CHECKED_IN);
            update(c, id, Status.CHECKED_OUT, r.amountPaid(), null);
            return load(c, id, false);
        });
    }

    // ---------- relatórios ----------

    public String occupancyCsv(LocalDate date) {
        return db.tx(c -> {
            StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
            int occupied = 0;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT r.room_number, m.type, r.id, r.check_in, r.check_out FROM reservations r "
                            + "JOIN rooms m ON m.number = r.room_number "
                            + "WHERE r.status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') "
                            + "AND r.check_in <= ? AND r.check_out > ? ORDER BY r.room_number, r.id")) {
                ps.setDate(1, Date.valueOf(date));
                ps.setDate(2, Date.valueOf(date));
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
            BigDecimal sum = Money.ZERO;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, room_number, check_in, check_out, status, amount_paid - COALESCE(refund, 0) AS amount "
                            + "FROM reservations WHERE check_in >= ? AND check_in < ? "
                            + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id")) {
                ps.setDate(1, Date.valueOf(month.atDay(1)));
                ps.setDate(2, Date.valueOf(month.plusMonths(1).atDay(1)));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal amount = Money.round(rs.getBigDecimal(6));
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
        for (Status s : allowed) {
            if (r.status() == s) {
                return;
            }
        }
        throw ApiException.notAllowed("Não é possível " + action + " uma reserva com situação " + r.status() + ".");
    }

    private static void update(Connection c, long id, Status status, BigDecimal paid, BigDecimal refund) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, status.name());
            ps.setBigDecimal(2, paid);
            ps.setBigDecimal(3, refund);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    private static Room findRoom(Connection c, int number, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ApiException(404, "ROOM_NOT_FOUND", "Quarto " + number + " não existe.");
                }
                return room(rs);
            }
        }
    }

    private static Room room(ResultSet rs) throws SQLException {
        return new Room(rs.getInt(1), RoomType.valueOf(rs.getString(2)), rs.getInt(3), Money.round(rs.getBigDecimal(4)));
    }

    private static Reservation load(Connection c, long id, boolean lock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT " + RES_COLS + " FROM reservations WHERE id = ?" + (lock ? " FOR UPDATE" : ""))) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new ApiException(404, "RESERVATION_NOT_FOUND", "Reserva " + id + " não existe.");
                }
                BigDecimal refund = rs.getBigDecimal(17);
                return new Reservation(rs.getLong(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getDate(6).toLocalDate(), rs.getDate(7).toLocalDate(), rs.getInt(8), rs.getString(9),
                        rs.getString(10), rs.getInt(11), Money.round(rs.getBigDecimal(12)), Money.round(rs.getBigDecimal(13)),
                        Money.round(rs.getBigDecimal(14)), Status.valueOf(rs.getString(15)),
                        Money.round(rs.getBigDecimal(16)), refund == null ? null : Money.round(refund));
            }
        }
    }
}
