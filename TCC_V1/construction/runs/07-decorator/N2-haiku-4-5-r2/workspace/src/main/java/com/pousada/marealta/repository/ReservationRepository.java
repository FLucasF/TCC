package com.pousada.marealta.repository;

import com.pousada.marealta.config.DatabaseConfig;
import com.pousada.marealta.domain.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private static final Logger logger = LoggerFactory.getLogger(ReservationRepository.class);

    public Reservation save(Reservation reservation) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = """
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                    check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, reservation.getRoomNumber());
                stmt.setString(2, reservation.getGuestName());
                stmt.setString(3, reservation.getGuestEmail());
                stmt.setString(4, reservation.getGuestCpf());
                stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
                stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
                stmt.setInt(7, reservation.getGuests());
                stmt.setString(8, reservation.getRatePlan().name());
                stmt.setInt(9, reservation.getNights());
                stmt.setBigDecimal(10, reservation.getSubtotal());
                stmt.setBigDecimal(11, reservation.getDiscount());
                stmt.setBigDecimal(12, reservation.getTotal());
                stmt.setString(13, reservation.getStatus().name());

                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        reservation.setId(rs.getInt(1));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao salvar reserva", e);
            throw new RuntimeException(e);
        }
        return reservation;
    }

    public void update(Reservation reservation) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = """
                UPDATE reservations SET status = ?, amount_paid = ?, refund = ?
                WHERE id = ?
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, reservation.getStatus().name());
                stmt.setBigDecimal(2, reservation.getAmountPaid());
                stmt.setBigDecimal(3, reservation.getRefund());
                stmt.setInt(4, reservation.getId());
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            logger.error("Erro ao atualizar reserva", e);
            throw new RuntimeException(e);
        }
    }

    public Optional<Reservation> findById(int id) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                       guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations WHERE id = ?
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRow(rs));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao buscar reserva", e);
        }
        return Optional.empty();
    }

    public List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                       guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE room_number = ? AND status != 'CANCELLED'
                AND check_in < ? AND check_out > ?
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, roomNumber);
                stmt.setDate(2, Date.valueOf(checkOut));
                stmt.setDate(3, Date.valueOf(checkIn));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        reservations.add(mapRow(rs));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao buscar reservas por período", e);
        }
        return reservations;
    }

    public List<Reservation> findByMonth(int year, int month) {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                       guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ?
                AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                ORDER BY check_in, id
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, year);
                stmt.setInt(2, month);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        reservations.add(mapRow(rs));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao buscar reservas por mês", e);
        }
        return reservations;
    }

    public List<Reservation> findByDateAndStatus(LocalDate date, Reservation.Status status) {
        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection()) {
            String sql = """
                SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                       guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
                FROM reservations
                WHERE check_in <= ? AND check_out > ? AND status = ?
                ORDER BY room_number
                """;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDate(1, Date.valueOf(date));
                stmt.setDate(2, Date.valueOf(date));
                stmt.setString(3, status.name());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        reservations.add(mapRow(rs));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao buscar reservas por data", e);
        }
        return reservations;
    }

    private Reservation mapRow(ResultSet rs) throws Exception {
        Reservation r = new Reservation(
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            Reservation.RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total")
        );
        r.setId(rs.getInt("id"));
        r.setStatus(Reservation.Status.valueOf(rs.getString("status")));
        r.setAmountPaid(rs.getBigDecimal("amount_paid"));
        BigDecimal refund = rs.getBigDecimal("refund");
        if (refund != null) {
            r.setRefund(refund);
        }
        return r;
    }
}
