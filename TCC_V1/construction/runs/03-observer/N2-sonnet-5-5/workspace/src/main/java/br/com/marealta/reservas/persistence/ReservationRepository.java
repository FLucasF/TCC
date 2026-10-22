package br.com.marealta.reservas.persistence;

import br.com.marealta.reservas.domain.Quote;
import br.com.marealta.reservas.domain.RatePlan;
import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.ReservationStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private static final String COLUMNS = "id, room_number, guest_name, guest_email, guest_cpf, check_in, "
            + "check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund";

    private final Database db;

    public ReservationRepository(Database db) {
        this.db = db;
    }

    public void insert(Reservation r) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
                        + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.roomNumber());
            ps.setString(2, r.guestName());
            ps.setString(3, r.guestEmail());
            ps.setString(4, r.guestCpf());
            ps.setObject(5, r.checkIn());
            ps.setObject(6, r.checkOut());
            ps.setInt(7, r.guests());
            ps.setString(8, r.ratePlan().name());
            ps.setInt(9, r.quote().nights());
            ps.setBigDecimal(10, r.quote().subtotal());
            ps.setBigDecimal(11, r.quote().discount());
            ps.setBigDecimal(12, r.quote().total());
            ps.setString(13, r.status().name());
            ps.setBigDecimal(14, r.amountPaid());
            ps.setBigDecimal(15, r.refund());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                r.assignId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao gravar reserva", e);
        }
    }

    public void update(Reservation r) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            ps.setString(1, r.status().name());
            ps.setBigDecimal(2, r.amountPaid());
            ps.setBigDecimal(3, r.refund());
            ps.setLong(4, r.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao atualizar reserva", e);
        }
    }

    public Optional<Reservation> find(long id) {
        return findOne("SELECT " + COLUMNS + " FROM reservations WHERE id = ?", id);
    }

    public Optional<Reservation> findForUpdate(long id) {
        return findOne("SELECT " + COLUMNS + " FROM reservations WHERE id = ? FOR UPDATE", id);
    }

    /** Há reserva não cancelada do quarto que cruza alguma noite de [checkIn, checkOut)? */
    public boolean hasActiveOverlap(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        try (PreparedStatement ps = db.connection().prepareStatement(
                "SELECT 1 FROM reservations WHERE room_number = ? AND status <> 'CANCELLED' "
                        + "AND check_in < ? AND check_out > ? LIMIT 1")) {
            ps.setInt(1, roomNumber);
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao verificar disponibilidade", e);
        }
    }

    /** Reservas pagas que ocupam o quarto na noite indicada, por número do quarto. */
    public List<Reservation> findPaidOccupying(LocalDate night) {
        return findMany("SELECT " + COLUMNS + " FROM reservations "
                + "WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') AND check_in <= ? AND check_out > ? "
                + "ORDER BY room_number", night, night);
    }

    /** Reservas que começam no mês e ficaram com dinheiro da pousada. */
    public List<Reservation> findKeptStartingIn(YearMonth month) {
        return findMany("SELECT " + COLUMNS + " FROM reservations "
                + "WHERE check_in >= ? AND check_in < ? AND amount_paid - COALESCE(refund, 0) > 0 "
                + "ORDER BY check_in, id", month.atDay(1), month.plusMonths(1).atDay(1));
    }

    private Optional<Reservation> findOne(String sql, long id) {
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar reserva", e);
        }
    }

    private List<Reservation> findMany(String sql, Object... params) {
        try (PreparedStatement ps = db.connection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao listar reservas", e);
        }
    }

    private static Reservation map(ResultSet rs) throws SQLException {
        Quote quote = new Quote(rs.getInt("nights"), rs.getBigDecimal("subtotal"), rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"));
        return Reservation.restore(rs.getLong("id"), rs.getInt("room_number"), rs.getString("guest_name"),
                rs.getString("guest_email"), rs.getString("guest_cpf"), rs.getObject("check_in", LocalDate.class),
                rs.getObject("check_out", LocalDate.class), rs.getInt("guests"),
                RatePlan.valueOf(rs.getString("rate_plan")), quote,
                ReservationStatus.valueOf(rs.getString("status")), rs.getBigDecimal("amount_paid"),
                rs.getBigDecimal("refund"));
    }
}
