package com.pousadamarealta.infrastructure.database;

import com.pousadamarealta.domain.model.RatePlanType;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.ReservationStatus;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final DataSource dataSource;

    public ReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Reservation save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan, company_code, nights, subtotal,
                discount, total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setReservationParameters(stmt, reservation);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }

            return reservation;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar reserva", e);
        }
    }

    public Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());

            if (reservation.getRefund() != null) {
                stmt.setBigDecimal(3, reservation.getRefund());
            } else {
                stmt.setNull(3, Types.DECIMAL);
            }

            stmt.setLong(4, reservation.getId());

            stmt.executeUpdate();
            return reservation;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar reserva", e);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                guests, rate_plan, company_code, nights, subtotal, discount, total,
                status, amount_paid, refund
            FROM reservations WHERE id = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar reserva", e);
        }

        return Optional.empty();
    }

    public List<Reservation> findActiveReservationsByRoom(String roomNumber) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                guests, rate_plan, company_code, nights, subtotal, discount, total,
                status, amount_paid, refund
            FROM reservations
            WHERE room_number = ? AND status != 'CANCELLED'
        """;

        return executeQuery(sql, roomNumber);
    }

    public List<Reservation> findPaidReservationsForNight(LocalDate night) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                guests, rate_plan, company_code, nights, subtotal, discount, total,
                status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                AND check_in <= ? AND check_out > ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            return executeQuery(stmt);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar reservas para a noite", e);
        }
    }

    public List<Reservation> findReservationsStartingInMonth(int year, int month) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                guests, rate_plan, company_code, nights, subtotal, discount, total,
                status, amount_paid, refund
            FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            return executeQuery(stmt);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar reservas do mês", e);
        }
    }

    private List<Reservation> executeQuery(String sql, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            return executeQuery(stmt);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao executar query", e);
        }
    }

    private List<Reservation> executeQuery(PreparedStatement stmt) throws Exception {
        List<Reservation> reservations = new ArrayList<>();

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        }

        return reservations;
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation reservation) throws Exception {
        stmt.setString(1, reservation.getRoomNumber());
        stmt.setString(2, reservation.getGuestName());
        stmt.setString(3, reservation.getGuestEmail());
        stmt.setString(4, reservation.getGuestCpf());
        stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
        stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
        stmt.setInt(7, reservation.getGuests());
        stmt.setString(8, reservation.getRatePlan().name());
        stmt.setString(9, reservation.getCompanyCode());
        stmt.setInt(10, reservation.getNights());
        stmt.setBigDecimal(11, reservation.getSubtotal());
        stmt.setBigDecimal(12, reservation.getDiscount());
        stmt.setBigDecimal(13, reservation.getTotal());
        stmt.setString(14, reservation.getStatus().name());
        stmt.setBigDecimal(15, reservation.getAmountPaid());

        if (reservation.getRefund() != null) {
            stmt.setBigDecimal(16, reservation.getRefund());
        } else {
            stmt.setNull(16, Types.DECIMAL);
        }
    }

    private Reservation mapReservation(ResultSet rs) throws Exception {
        Reservation reservation = new Reservation(
            rs.getString("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlanType.valueOf(rs.getString("rate_plan")),
            rs.getString("company_code"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total")
        );

        reservation.setId(rs.getLong("id"));

        String status = rs.getString("status");
        if ("CONFIRMED".equals(status)) {
            reservation.confirm();
        } else if ("CHECKED_IN".equals(status)) {
            reservation.confirm();
            reservation.checkIn();
        } else if ("CHECKED_OUT".equals(status)) {
            reservation.confirm();
            reservation.checkIn();
            reservation.checkOut();
        } else if ("CANCELLED".equals(status)) {
            BigDecimal refund = rs.getBigDecimal("refund");
            reservation.cancel(refund);
        }

        return reservation;
    }
}
