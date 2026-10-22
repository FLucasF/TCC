package br.com.marealta.infra;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;
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

public class ReservationRepository {

    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private static final String PAID_STATUSES = "('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')";

    /** Trava as reservas do quarto até o fim da transação, para duas reservas não disputarem a mesma noite. */
    public void lockRoom(Connection connection, int roomNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            statement.setLong(1, roomNumber);
            statement.execute();
        }
    }

    public boolean isRoomBooked(Connection connection, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                        + "AND check_in < ? AND check_out > ? LIMIT 1")) {
            statement.setInt(1, roomNumber);
            statement.setDate(2, Date.valueOf(checkOut));
            statement.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    public Reservation insert(Connection connection, Reservation r) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                        + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, r.roomNumber());
            statement.setString(2, r.guestName());
            statement.setString(3, r.guestEmail());
            statement.setString(4, r.guestCpf());
            statement.setDate(5, Date.valueOf(r.checkIn()));
            statement.setDate(6, Date.valueOf(r.checkOut()));
            statement.setInt(7, r.guests());
            statement.setString(8, r.ratePlan());
            statement.setInt(9, r.nights());
            statement.setBigDecimal(10, r.subtotal());
            statement.setBigDecimal(11, r.discount());
            statement.setBigDecimal(12, r.total());
            statement.setString(13, r.status().name());
            statement.setBigDecimal(14, r.amountPaid());
            statement.setBigDecimal(15, r.refund());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return r.withId(keys.getLong("id"));
            }
        }
    }

    public void update(Connection connection, Reservation r) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            statement.setString(1, r.status().name());
            statement.setBigDecimal(2, r.amountPaid());
            statement.setBigDecimal(3, r.refund());
            statement.setLong(4, r.id());
            statement.executeUpdate();
        }
    }

    public Optional<Reservation> find(Connection connection, long id) throws SQLException {
        return findOne(connection, "SELECT " + COLUMNS + " FROM reservations WHERE id = ?", id);
    }

    /** Busca e trava a linha até o fim da transação. */
    public Optional<Reservation> findForUpdate(Connection connection, long id) throws SQLException {
        return findOne(connection, "SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE", id);
    }

    /** Reservas pagas que ocupam um quarto na noite: quem sai naquele dia não conta. */
    public List<Reservation> findPaidOccupying(Connection connection, LocalDate night) throws SQLException {
        return findMany(connection, "SELECT " + COLUMNS + " FROM reservations WHERE status IN " + PAID_STATUSES
                + " AND check_in <= ? AND check_out > ? ORDER BY room_number, id", night, night);
    }

    /** Reservas que começam no período e ainda deixam dinheiro com a pousada (pago menos devolvido). */
    public List<Reservation> findWithRevenueStartingBetween(Connection connection, LocalDate from, LocalDate until)
            throws SQLException {
        return findMany(connection, "SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ? "
                + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id", from, until);
    }

    private Optional<Reservation> findOne(Connection connection, String sql, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(map(rows)) : Optional.empty();
            }
        }
    }

    private List<Reservation> findMany(Connection connection, String sql, LocalDate first, LocalDate second)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(first));
            statement.setDate(2, Date.valueOf(second));
            try (ResultSet rows = statement.executeQuery()) {
                List<Reservation> found = new ArrayList<>();
                while (rows.next()) {
                    found.add(map(rows));
                }
                return found;
            }
        }
    }

    private Reservation map(ResultSet rows) throws SQLException {
        return new Reservation(
                rows.getLong("id"),
                rows.getInt("room_number"),
                rows.getString("guest_name"),
                rows.getString("guest_email"),
                rows.getString("guest_cpf"),
                rows.getDate("check_in").toLocalDate(),
                rows.getDate("check_out").toLocalDate(),
                rows.getInt("guests"),
                rows.getString("rate_plan"),
                rows.getInt("nights"),
                rows.getBigDecimal("subtotal"),
                rows.getBigDecimal("discount"),
                rows.getBigDecimal("total"),
                ReservationStatus.valueOf(rows.getString("status")),
                rows.getBigDecimal("amount_paid"),
                rows.getBigDecimal("refund"));
    }
}
