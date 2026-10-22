package br.com.marealta.pousada.persistence;

import br.com.marealta.pousada.domain.Quote;
import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationStatus;
import br.com.marealta.pousada.domain.Rooms;
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

public final class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final Connection connection;

    ReservationRepository(Connection connection) {
        this.connection = connection;
    }

    /** Serializa as reservas de um mesmo quarto até o fim da transação. */
    public void lockRoom(int roomNumber) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            statement.setLong(1, roomNumber);
            statement.execute();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Há reserva não cancelada do quarto que ocupe alguma noite de [checkIn, checkOut)? */
    public boolean isRoomTaken(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                + "AND check_in < ? AND check_out > ? LIMIT 1";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomNumber);
            statement.setDate(2, Date.valueOf(checkOut));
            statement.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Reservation insert(Reservation reservation) {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, reservation.room().number());
            statement.setString(2, reservation.guestName());
            statement.setString(3, reservation.guestEmail());
            statement.setString(4, reservation.guestCpf());
            statement.setDate(5, Date.valueOf(reservation.checkIn()));
            statement.setDate(6, Date.valueOf(reservation.checkOut()));
            statement.setInt(7, reservation.guests());
            statement.setString(8, reservation.ratePlan().name());
            statement.setInt(9, reservation.nights());
            statement.setBigDecimal(10, reservation.subtotal());
            statement.setBigDecimal(11, reservation.discount());
            statement.setBigDecimal(12, reservation.total());
            statement.setString(13, reservation.status().name());
            statement.setBigDecimal(14, reservation.amountPaid());
            statement.setBigDecimal(15, reservation.refund());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return reservation.withId(keys.getLong("id"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public void update(Reservation reservation) {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reservation.status().name());
            statement.setBigDecimal(2, reservation.amountPaid());
            statement.setBigDecimal(3, reservation.refund());
            statement.setLong(4, reservation.id());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    public Optional<Reservation> find(long id) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE id = ?", id).stream().findFirst();
    }

    /** Busca e trava a linha até o fim da transação, para operações que mudam a situação. */
    public Optional<Reservation> findForUpdate(long id) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE", id).stream().findFirst();
    }

    /** Reservas pagas que ocupam o quarto na noite: entra quem chega nela, não quem sai nela. */
    public List<Reservation> findPaidOccupying(LocalDate night) {
        String statuses = String.join(",", ReservationStatus.paid().stream().map(s -> "'" + s + "'").toList());
        return query("SELECT " + COLUMNS + " FROM reservations WHERE status IN (" + statuses + ") "
                + "AND check_in <= ? AND check_out > ? ORDER BY room_number, id", Date.valueOf(night), Date.valueOf(night));
    }

    /** Reservas que começam no período e deixaram dinheiro com a pousada. */
    public List<Reservation> findKeepingMoneyStartingBetween(LocalDate first, LocalDate last) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE check_in BETWEEN ? AND ? "
                + "AND amount_paid - COALESCE(refund, 0) > 0 ORDER BY check_in, id", Date.valueOf(first), Date.valueOf(last));
    }

    private List<Reservation> query(String sql, Object... parameters) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet rows = statement.executeQuery()) {
                List<Reservation> found = new ArrayList<>();
                while (rows.next()) {
                    found.add(read(rows));
                }
                return found;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Reservation read(ResultSet rows) throws SQLException {
        Quote quote = new Quote(rows.getInt("nights"), rows.getBigDecimal("subtotal"),
                rows.getBigDecimal("discount"), rows.getBigDecimal("total"));
        return new Reservation(
                rows.getLong("id"),
                Rooms.find(rows.getInt("room_number")).orElseThrow(),
                rows.getString("guest_name"),
                rows.getString("guest_email"),
                rows.getString("guest_cpf"),
                rows.getDate("check_in").toLocalDate(),
                rows.getDate("check_out").toLocalDate(),
                rows.getInt("guests"),
                RatePlan.valueOf(rows.getString("rate_plan")),
                quote,
                ReservationStatus.valueOf(rows.getString("status")),
                rows.getBigDecimal("amount_paid"),
                rows.getBigDecimal("refund"));
    }
}
