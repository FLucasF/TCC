package com.pousadamarealta.infrastructure;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final Database database;

    public ReservationRepository(Database database) {
        this.database = database;
    }

    public Reservation save(Reservation reservation) {
        if (reservation.getId() == null) {
            return insert(reservation);
        } else {
            update(reservation);
            return reservation;
        }
    }

    private Reservation insert(Reservation reservation) {
        String query = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                                     check_in, check_out, guests, rate_plan, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            setReservationParameters(stmt, reservation);
            stmt.executeUpdate();

            try (var rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir reserva", e);
        }
    }

    private void update(Reservation reservation) {
        String query = """
            UPDATE reservations
            SET room_number = ?, guest_name = ?, guest_email = ?, guest_cpf = ?,
                check_in = ?, check_out = ?, guests = ?, rate_plan = ?,
                status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(query)) {

            setReservationParameters(stmt, reservation);
            stmt.setLong(12, reservation.getId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar reserva", e);
        }
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation reservation) throws SQLException {
        stmt.setString(1, reservation.getRoomNumber());
        stmt.setString(2, reservation.getGuestName());
        stmt.setString(3, reservation.getGuestEmail());
        stmt.setString(4, reservation.getGuestCpf());
        stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
        stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
        stmt.setInt(7, reservation.getGuests());
        stmt.setString(8, reservation.getRatePlan().name());
        stmt.setString(9, reservation.getStatus().name());
        stmt.setBigDecimal(10, reservation.getAmountPaid());

        if (reservation.getRefund() != null) {
            stmt.setBigDecimal(11, reservation.getRefund());
        } else {
            stmt.setNull(11, Types.DECIMAL);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String query = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, status, amount_paid, refund
            FROM reservations
            WHERE id = ?
            """;

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(query)) {

            stmt.setLong(1, id);
            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar reserva", e);
        }

        return Optional.empty();
    }

    public List<Reservation> findConflicting(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        String query = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
              AND (? IS NULL OR id != ?)
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(query)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            if (excludeId != null) {
                stmt.setLong(4, excludeId);
                stmt.setLong(5, excludeId);
            } else {
                stmt.setNull(4, Types.BIGINT);
                stmt.setNull(5, Types.BIGINT);
            }

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar reservas conflitantes", e);
        }

        return reservations;
    }

    public List<Reservation> findByMonth(YearMonth month) {
        String query = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ? AND check_in < ?
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.plusMonths(1).atDay(1);

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(query)) {

            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar reservas do mês", e);
        }

        return reservations;
    }

    public List<Reservation> findByNight(LocalDate night) {
        String query = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, status, amount_paid, refund
            FROM reservations
            WHERE check_in <= ? AND check_out > ?
              AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(query)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar reservas da noite", e);
        }

        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String roomNumber = rs.getString("room_number");
        String guestName = rs.getString("guest_name");
        String guestEmail = rs.getString("guest_email");
        String guestCpf = rs.getString("guest_cpf");
        LocalDate checkIn = rs.getDate("check_in").toLocalDate();
        LocalDate checkOut = rs.getDate("check_out").toLocalDate();
        int guests = rs.getInt("guests");
        RatePlan ratePlan = RatePlan.valueOf(rs.getString("rate_plan"));
        ReservationStatus status = ReservationStatus.valueOf(rs.getString("status"));
        BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
        BigDecimal refund = rs.getBigDecimal("refund");

        return new Reservation(id, roomNumber, guestName, guestEmail, guestCpf,
                              checkIn, checkOut, guests, ratePlan, status, amountPaid, refund);
    }
}
