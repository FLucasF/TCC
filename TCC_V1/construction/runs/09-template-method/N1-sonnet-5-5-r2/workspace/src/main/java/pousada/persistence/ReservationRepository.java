package pousada.persistence;

import pousada.domain.RatePlan;
import pousada.domain.Reservation;
import pousada.domain.ReservationStatus;

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

    private final Connection connection;

    ReservationRepository(Connection connection) {
        this.connection = connection;
    }

    /** Há reserva não cancelada do quarto que ocupe alguma noite de [checkIn, checkOut)? */
    public boolean hasConflict(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                + "AND check_in < ? AND check_out > ? LIMIT 1";
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setInt(1, roomNumber);
            st.setDate(2, Date.valueOf(checkOut));
            st.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro no banco de dados", e);
        }
    }

    public Reservation insert(Reservation r) {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement st = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.setInt(1, r.roomNumber());
            st.setString(2, r.guestName());
            st.setString(3, r.guestEmail());
            st.setString(4, r.guestCpf());
            st.setDate(5, Date.valueOf(r.checkIn()));
            st.setDate(6, Date.valueOf(r.checkOut()));
            st.setInt(7, r.guests());
            st.setString(8, r.ratePlan().name());
            st.setInt(9, r.nights());
            st.setBigDecimal(10, r.subtotal());
            st.setBigDecimal(11, r.discount());
            st.setBigDecimal(12, r.total());
            st.setString(13, r.status().name());
            st.setBigDecimal(14, r.amountPaid());
            st.setBigDecimal(15, r.refund());
            st.executeUpdate();
            try (ResultSet keys = st.getGeneratedKeys()) {
                keys.next();
                return find(keys.getLong("id")).orElseThrow();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro no banco de dados", e);
        }
    }

    public Optional<Reservation> find(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM reservations WHERE id = ?", id);
    }

    /** Busca e trava a linha até o fim da transação, para mudanças de situação não se atropelarem. */
    public Optional<Reservation> findForUpdate(long id) {
        return queryOne("SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE", id);
    }

    public void update(Reservation r) {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?";
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setString(1, r.status().name());
            st.setBigDecimal(2, r.amountPaid());
            st.setBigDecimal(3, r.refund());
            st.setLong(4, r.id());
            st.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro no banco de dados", e);
        }
    }

    /** Reservas que ocupam a noite, em ordem de quarto. */
    public List<Reservation> occupying(LocalDate night) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE check_in <= ? AND check_out > ? "
                + "ORDER BY room_number, id", Date.valueOf(night), Date.valueOf(night));
    }

    /** Reservas com entrada em [from, toExclusive), em ordem de entrada e número. */
    public List<Reservation> startingIn(LocalDate from, LocalDate toExclusive) {
        return query("SELECT " + COLUMNS + " FROM reservations WHERE check_in >= ? AND check_in < ? "
                + "ORDER BY check_in, id", Date.valueOf(from), Date.valueOf(toExclusive));
    }

    private Optional<Reservation> queryOne(String sql, long id) {
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setLong(1, id);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro no banco de dados", e);
        }
    }

    private List<Reservation> query(String sql, Date first, Date second) {
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setDate(1, first);
            st.setDate(2, second);
            try (ResultSet rs = st.executeQuery()) {
                List<Reservation> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(read(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro no banco de dados", e);
        }
    }

    private Reservation read(ResultSet rs) throws SQLException {
        return new Reservation(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"), RatePlan.valueOf(rs.getString("rate_plan")), rs.getInt("nights"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"), rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"), rs.getBigDecimal("refund"));
    }
}
