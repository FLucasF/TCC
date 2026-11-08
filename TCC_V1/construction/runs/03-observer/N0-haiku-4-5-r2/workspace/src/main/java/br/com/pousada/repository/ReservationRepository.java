package br.com.pousada.repository;

import br.com.pousada.config.DatabaseConfig;
import br.com.pousada.model.Reservation;
import br.com.pousada.model.ReservationStatus;
import br.com.pousada.model.RatePlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private static final Logger logger = LoggerFactory.getLogger(ReservationRepository.class);

    public Reservation save(Reservation reservation) {
        if (reservation.id == 0) {
            return insert(reservation);
        } else {
            update(reservation);
            return reservation;
        }
    }

    private Reservation insert(Reservation r) {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                                      guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, r.roomNumber);
            stmt.setString(2, r.guestName);
            stmt.setString(3, r.guestEmail);
            stmt.setString(4, r.guestCpf);
            stmt.setObject(5, r.checkIn);
            stmt.setObject(6, r.checkOut);
            stmt.setInt(7, r.guests);
            stmt.setString(8, r.ratePlan.toString());
            stmt.setLong(9, r.nights);
            stmt.setBigDecimal(10, new java.math.BigDecimal(r.subtotal));
            stmt.setBigDecimal(11, new java.math.BigDecimal(r.discount));
            stmt.setBigDecimal(12, new java.math.BigDecimal(r.total));
            stmt.setString(13, r.status.toString());
            stmt.setBigDecimal(14, new java.math.BigDecimal(r.amountPaid));
            stmt.setObject(15, r.refund != null ? new java.math.BigDecimal(r.refund) : null);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    r.id = rs.getLong("id");
                }
            }
        } catch (SQLException e) {
            logger.error("Erro ao inserir reserva", e);
            throw new RuntimeException(e);
        }
        return r;
    }

    private void update(Reservation r) {
        String sql = """
            UPDATE reservations SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, r.status.toString());
            stmt.setBigDecimal(2, new java.math.BigDecimal(r.amountPaid));
            stmt.setObject(3, r.refund != null ? new java.math.BigDecimal(r.refund) : null);
            stmt.setLong(4, r.id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Erro ao atualizar reserva", e);
            throw new RuntimeException(e);
        }
    }

    public Optional<Reservation> findById(long id) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations WHERE id = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Erro ao buscar reserva", e);
        }
        return Optional.empty();
    }

    public List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate startDate, LocalDate endDate) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ? AND status != 'CANCELLED'
            AND check_in <= ? AND check_out > ?
            ORDER BY check_in
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, roomNumber);
            stmt.setObject(2, endDate);
            stmt.setObject(3, startDate);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Erro ao buscar reservas por quarto e período", e);
        }
        return reservations;
    }

    public List<Reservation> findOccupancyByDate(LocalDate date) {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ? AND check_out > ?
            ORDER BY room_number
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, date);
            stmt.setObject(2, date);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Erro ao buscar ocupação", e);
        }
        return reservations;
    }

    public List<Reservation> findByMonthWithPayment(YearMonth month) {
        List<Reservation> reservations = new ArrayList<>();
        LocalDate startOfMonth = month.atDay(1);
        LocalDate endOfMonth = month.atEndOfMonth();

        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
                   rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ? AND check_in <= ? AND amount_paid > 0
            ORDER BY check_in, id
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, startOfMonth);
            stmt.setObject(2, endOfMonth);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Erro ao buscar reservas financeiras", e);
        }
        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Double refund = rs.getObject("refund") != null ? rs.getDouble("refund") : null;
        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getObject("check_in", LocalDate.class),
            rs.getObject("check_out", LocalDate.class),
            rs.getInt("guests"),
            RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getLong("nights"),
            rs.getDouble("subtotal"),
            rs.getDouble("discount"),
            rs.getDouble("total"),
            ReservationStatus.valueOf(rs.getString("status")),
            rs.getDouble("amount_paid"),
            refund
        );
    }
}
