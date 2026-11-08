package br.com.marealta.persistence;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;

public class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, company_code, nights, subtotal, discount, status, amount_paid, refund";

    /** Serializa as reservas de um mesmo quarto até o fim da transação. */
    public void lockRoom(Connection connection, int roomNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            statement.setLong(1, roomNumber);
            statement.execute();
        }
    }

    public boolean hasActiveReservation(Connection connection, int roomNumber, LocalDate checkIn, LocalDate checkOut)
            throws SQLException {
        String sql = "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                + "AND check_in < ? AND check_out > ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomNumber);
            statement.setObject(2, checkOut);
            statement.setObject(3, checkIn);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    /** Grava a reserva nova e devolve-a já com o id gerado. */
    public Reservation insert(Connection connection, Reservation draft) throws SQLException {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                + "guests, rate_plan, company_code, nights, subtotal, discount, status, amount_paid, refund) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 1;
            statement.setInt(i++, draft.roomNumber());
            statement.setString(i++, draft.guestName());
            statement.setString(i++, draft.guestEmail());
            statement.setString(i++, draft.guestCpf());
            statement.setObject(i++, draft.checkIn());
            statement.setObject(i++, draft.checkOut());
            statement.setInt(i++, draft.guests());
            statement.setString(i++, draft.ratePlan());
            statement.setString(i++, draft.companyCode());
            statement.setInt(i++, draft.nights());
            statement.setBigDecimal(i++, draft.subtotal());
            statement.setBigDecimal(i++, draft.discount());
            statement.setString(i++, draft.status().name());
            statement.setBigDecimal(i++, draft.amountPaid());
            statement.setBigDecimal(i, draft.refund());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return find(connection, keys.getLong("id"), false).orElseThrow();
            }
        }
    }

    public void update(Connection connection, Reservation reservation) throws SQLException {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reservation.status().name());
            statement.setBigDecimal(2, reservation.amountPaid());
            statement.setBigDecimal(3, reservation.refund());
            statement.setLong(4, reservation.id());
            statement.executeUpdate();
        }
    }

    public Optional<Reservation> find(Connection connection, long id, boolean forUpdate) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM reservations WHERE id = ?" + (forUpdate ? " FOR UPDATE" : "");
        return queryList(connection, sql, id).stream().findFirst();
    }

    /** Reservas pagas e não canceladas que ocupam o quarto na noite, por número de quarto. */
    public List<Reservation> findPaidOccupying(Connection connection, LocalDate night) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM reservations WHERE status IN ('CONFIRMED','CHECKED_IN','CHECKED_OUT') "
                + "AND check_in <= ? AND check_out > ? ORDER BY room_number, id";
        return queryList(connection, sql, night, night);
    }

    /** Reservas que deixaram dinheiro com a pousada e começam no período [from, to). */
    public List<Reservation> findWithRevenueStartingBetween(Connection connection, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ? "
                + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id";
        return queryList(connection, sql, from, to);
    }

    private List<Reservation> queryList(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet rows = statement.executeQuery()) {
                List<Reservation> result = new ArrayList<>();
                while (rows.next()) {
                    result.add(map(rows));
                }
                return result;
            }
        }
    }

    private Reservation map(ResultSet row) throws SQLException {
        BigDecimal refund = row.getBigDecimal("refund");
        return new Reservation(row.getLong("id"), row.getInt("room_number"), row.getString("guest_name"),
                row.getString("guest_email"), row.getString("guest_cpf"), row.getObject("check_in", LocalDate.class),
                row.getObject("check_out", LocalDate.class), row.getInt("guests"), row.getString("rate_plan"),
                row.getString("company_code"), row.getInt("nights"), row.getBigDecimal("subtotal"),
                row.getBigDecimal("discount"), ReservationStatus.valueOf(row.getString("status")),
                row.getBigDecimal("amount_paid"), refund);
    }
}
