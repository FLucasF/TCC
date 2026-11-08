package com.pousadamarealta.repository;

import com.pousadamarealta.database.Database;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
            return update(reservation);
        }
    }

    private Reservation insert(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan, nights, subtotal, discount, total,
                status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            setReservationParams(stmt, reservation);
            var rs = stmt.executeQuery();
            rs.next();
            reservation.setId(rs.getLong(1));
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir reserva", e);
        }
    }

    private Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET
                room_number = ?, guest_name = ?, guest_email = ?, guest_cpf = ?,
                check_in = ?, check_out = ?, guests = ?, rate_plan = ?, nights = ?,
                subtotal = ?, discount = ?, total = ?, status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            setReservationParams(stmt, reservation);
            stmt.setLong(16, reservation.getId());
            stmt.executeUpdate();
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar reserva", e);
        }
    }

    private void setReservationParams(java.sql.PreparedStatement stmt, Reservation r) throws SQLException {
        stmt.setString(1, r.getRoomNumber());
        stmt.setString(2, r.getGuestName());
        stmt.setString(3, r.getGuestEmail());
        stmt.setString(4, r.getGuestCpf());
        stmt.setDate(5, Date.valueOf(r.getCheckIn()));
        stmt.setDate(6, Date.valueOf(r.getCheckOut()));
        stmt.setInt(7, r.getGuests());
        stmt.setString(8, r.getRatePlan().name());
        stmt.setInt(9, r.getNights());
        stmt.setBigDecimal(10, r.getSubtotal());
        stmt.setBigDecimal(11, r.getDiscount());
        stmt.setBigDecimal(12, r.getTotal());
        stmt.setString(13, r.getStatus().name());
        stmt.setBigDecimal(14, r.getAmountPaid());
        stmt.setBigDecimal(15, r.getRefund());
    }

    public Optional<Reservation> findById(Long id) {
        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement("SELECT * FROM reservations WHERE id = ?")) {

            stmt.setLong(1, id);
            var rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapReservation(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar reserva", e);
        }
    }

    public boolean hasConflictingReservation(String roomNumber, LocalDate checkIn, LocalDate checkOut, Long excludeId) {
        String sql = """
            SELECT COUNT(*) FROM reservations
            WHERE room_number = ?
              AND status != 'CANCELLED'
              AND check_in < ?
              AND check_out > ?
              AND (? IS NULL OR id != ?)
            """;

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            stmt.setObject(4, excludeId);
            stmt.setObject(5, excludeId);

            var rs = stmt.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar disponibilidade", e);
        }
    }

    public List<Reservation> findByNight(LocalDate night) {
        String sql = """
            SELECT * FROM reservations
            WHERE check_in <= ? AND check_out > ?
              AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            ORDER BY room_number
            """;

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            var rs = stmt.executeQuery();
            List<Reservation> reservations = new ArrayList<>();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar reservas por noite", e);
        }
    }

    public List<Reservation> findByMonth(int year, int month) {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
              AND EXTRACT(MONTH FROM check_in) = ?
            ORDER BY check_in, id
            """;

        try (var conn = database.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            var rs = stmt.executeQuery();
            List<Reservation> reservations = new ArrayList<>();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar reservas por mês", e);
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getLong("id"));
        r.setRoomNumber(rs.getString("room_number"));
        r.setGuestName(rs.getString("guest_name"));
        r.setGuestEmail(rs.getString("guest_email"));
        r.setGuestCpf(rs.getString("guest_cpf"));
        r.setCheckIn(rs.getDate("check_in").toLocalDate());
        r.setCheckOut(rs.getDate("check_out").toLocalDate());
        r.setGuests(rs.getInt("guests"));
        r.setRatePlan(RatePlan.valueOf(rs.getString("rate_plan")));
        r.setNights(rs.getInt("nights"));
        r.setSubtotal(rs.getBigDecimal("subtotal"));
        r.setDiscount(rs.getBigDecimal("discount"));
        r.setTotal(rs.getBigDecimal("total"));
        r.setStatus(ReservationStatus.valueOf(rs.getString("status")));
        r.setAmountPaid(rs.getBigDecimal("amount_paid"));
        r.setRefund(rs.getBigDecimal("refund"));
        return r;
    }
}
