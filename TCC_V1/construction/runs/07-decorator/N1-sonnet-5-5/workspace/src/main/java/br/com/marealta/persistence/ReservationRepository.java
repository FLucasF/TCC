package br.com.marealta.persistence;

import br.com.marealta.domain.Guest;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.RatePlan;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class ReservationRepository {
    private static final String SELECT = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan,
                   nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations""";

    /** Grava a reserva nova e devolve o id gerado. */
    public long insert(Connection connection, Reservation r) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                    guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, r.roomNumber());
            statement.setString(2, r.guest().name());
            statement.setString(3, r.guest().email());
            statement.setString(4, r.guest().cpf());
            statement.setDate(5, Date.valueOf(r.arrival()));
            statement.setDate(6, Date.valueOf(r.departure()));
            statement.setInt(7, r.guests());
            statement.setString(8, r.ratePlan().name());
            statement.setInt(9, r.quote().nights());
            statement.setBigDecimal(10, r.quote().subtotal());
            statement.setBigDecimal(11, r.quote().discount());
            statement.setBigDecimal(12, r.quote().total());
            statement.setString(13, r.status().name());
            statement.setBigDecimal(14, r.amountPaid());
            statement.setBigDecimal(15, r.refund());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void updateState(Connection connection, Reservation r) throws SQLException {
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
        return findOne(connection, id, "");
    }

    public Optional<Reservation> findForUpdate(Connection connection, long id) throws SQLException {
        return findOne(connection, id, " FOR UPDATE");
    }

    private Optional<Reservation> findOne(Connection connection, long id, String suffix) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT + " WHERE id = ?" + suffix)) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(read(rows)) : Optional.empty();
            }
        }
    }

    /** Há alguma reserva não cancelada do quarto que ocupe alguma noite de [checkIn, checkOut)? */
    public boolean existsOverlapping(Connection connection, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM reservations
                WHERE room_number = ? AND status <> 'CANCELLED' AND check_in < ? AND check_out > ?
                LIMIT 1""")) {
            statement.setInt(1, roomNumber);
            statement.setDate(2, Date.valueOf(checkOut));
            statement.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    /** Reservas pagas que ocupam o quarto na noite indicada, em ordem de quarto. */
    public List<Reservation> findPaidOccupying(Connection connection, LocalDate night) throws SQLException {
        String[] paid = Arrays.stream(ReservationStatus.values()).filter(ReservationStatus::isPaid)
                .map(Enum::name).toArray(String[]::new);
        try (PreparedStatement statement = connection.prepareStatement(
                SELECT + " WHERE status = ANY (?) AND check_in <= ? AND check_out > ? ORDER BY room_number, id")) {
            statement.setArray(1, connection.createArrayOf("varchar", paid));
            statement.setDate(2, Date.valueOf(night));
            statement.setDate(3, Date.valueOf(night));
            return readAll(statement);
        }
    }

    /** Reservas com entrada em [from, to), em ordem de entrada e de número. */
    public List<Reservation> findStartingBetween(Connection connection, LocalDate from, LocalDate to)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                SELECT + " WHERE check_in >= ? AND check_in < ? ORDER BY check_in, id")) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            return readAll(statement);
        }
    }

    private List<Reservation> readAll(PreparedStatement statement) throws SQLException {
        try (ResultSet rows = statement.executeQuery()) {
            List<Reservation> found = new ArrayList<>();
            while (rows.next()) {
                found.add(read(rows));
            }
            return found;
        }
    }

    private Reservation read(ResultSet rows) throws SQLException {
        Quote quote = new Quote(rows.getInt("nights"), rows.getBigDecimal("subtotal"),
                rows.getBigDecimal("discount"), rows.getBigDecimal("total"));
        return new Reservation(rows.getLong("id"), rows.getInt("room_number"),
                new Guest(rows.getString("guest_name"), rows.getString("guest_email"),
                        rows.getString("guest_cpf")),
                rows.getDate("check_in").toLocalDate(), rows.getDate("check_out").toLocalDate(),
                rows.getInt("guests"), RatePlan.valueOf(rows.getString("rate_plan")), quote,
                ReservationStatus.valueOf(rows.getString("status")), rows.getBigDecimal("amount_paid"),
                rows.getBigDecimal("refund"));
    }
}
