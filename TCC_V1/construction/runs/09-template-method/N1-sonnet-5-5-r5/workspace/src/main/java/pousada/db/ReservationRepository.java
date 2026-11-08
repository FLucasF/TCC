package pousada.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import pousada.domain.NewReservation;
import pousada.domain.Pricing.Quote;
import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;
import pousada.domain.Money;

public class ReservationRepository {
    private static final String SELECT = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations""";

    private final Database db;

    public ReservationRepository(Database db) {
        this.db = db;
    }

    public Reservation insert(NewReservation n, Quote quote) {
        return db.use(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                        guests, rate_plan, nights, subtotal, discount, total, status, amount_paid)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    RETURNING id""")) {
                statement.setInt(1, n.roomNumber());
                statement.setString(2, n.guestName());
                statement.setString(3, n.guestEmail());
                statement.setString(4, n.guestCpf());
                statement.setObject(5, n.checkIn());
                statement.setObject(6, n.checkOut());
                statement.setInt(7, n.guests());
                statement.setString(8, n.ratePlan().name());
                statement.setInt(9, quote.nights());
                statement.setBigDecimal(10, quote.subtotal());
                statement.setBigDecimal(11, quote.discount());
                statement.setBigDecimal(12, quote.total());
                statement.setString(13, ReservationStatus.PENDING.name());
                statement.setBigDecimal(14, Money.ZERO);
                try (var rows = statement.executeQuery()) {
                    rows.next();
                    return new Reservation(rows.getLong(1), n.roomNumber(), n.guestName(), n.guestEmail(),
                            n.guestCpf(), n.checkIn(), n.checkOut(), n.guests(), n.ratePlan(), quote.nights(),
                            quote.subtotal(), quote.discount(), quote.total(), ReservationStatus.PENDING,
                            Money.ZERO, null);
                }
            }
        });
    }

    public void save(Reservation r) {
        db.use(connection -> {
            try (var statement = connection.prepareStatement(
                    "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
                statement.setString(1, r.status().name());
                statement.setBigDecimal(2, r.amountPaid());
                statement.setBigDecimal(3, r.refund());
                statement.setLong(4, r.id());
                return statement.executeUpdate();
            }
        });
    }

    public Optional<Reservation> find(long id) {
        return queryOne(SELECT + " WHERE id = ?", id);
    }

    /** Busca a reserva travando-a até o fim da transação. */
    public Optional<Reservation> lock(long id) {
        return queryOne(SELECT + " WHERE id = ? FOR UPDATE", id);
    }

    /** Há reserva não cancelada do quarto em alguma das noites de [checkIn, checkOut)? */
    public boolean existsOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        return db.use(connection -> {
            try (var statement = connection.prepareStatement("""
                    SELECT 1 FROM reservations
                    WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?
                    LIMIT 1""")) {
                statement.setInt(1, roomNumber);
                statement.setObject(2, checkOut);
                statement.setObject(3, checkIn);
                try (var rows = statement.executeQuery()) {
                    return rows.next();
                }
            }
        });
    }

    /** Reservas pagas que ocupam um quarto na noite, por número do quarto. */
    public List<Reservation> paidOccupying(LocalDate night) {
        return query(SELECT + " " + """
                 WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') AND check_in <= ? AND check_out > ?
                 ORDER BY room_number, id""", night, night);
    }

    /** Reservas que começam no mês, por data de entrada e número. */
    public List<Reservation> startingIn(YearMonth month) {
        return query(SELECT + " WHERE check_in >= ? AND check_in < ? ORDER BY check_in, id",
                month.atDay(1), month.plusMonths(1).atDay(1));
    }

    private Optional<Reservation> queryOne(String sql, Object... params) {
        return query(sql, params).stream().findFirst();
    }

    private List<Reservation> query(String sql, Object... params) {
        return db.use(connection -> {
            try (var statement = connection.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }
                try (var rows = statement.executeQuery()) {
                    List<Reservation> found = new ArrayList<>();
                    while (rows.next()) {
                        found.add(map(rows));
                    }
                    return found;
                }
            }
        });
    }

    private static Reservation map(ResultSet rows) throws SQLException {
        return new Reservation(
                rows.getLong("id"), rows.getInt("room_number"),
                rows.getString("guest_name"), rows.getString("guest_email"), rows.getString("guest_cpf"),
                rows.getObject("check_in", LocalDate.class), rows.getObject("check_out", LocalDate.class),
                rows.getInt("guests"), RatePlan.valueOf(rows.getString("rate_plan")), rows.getInt("nights"),
                rows.getBigDecimal("subtotal"), rows.getBigDecimal("discount"), rows.getBigDecimal("total"),
                ReservationStatus.valueOf(rows.getString("status")),
                rows.getBigDecimal("amount_paid"), rows.getBigDecimal("refund"));
    }
}
