package com.pousadamarealta.repository;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcReservationRepository implements ReservationRepository {
    private final DataSource dataSource;

    public JdbcReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Reservation save(Reservation reservation) {
        if (reservation.id() == null) {
            return insert(reservation);
        } else {
            update(reservation);
            return reservation;
        }
    }

    private Reservation insert(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                                     guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.roomNumber());
            stmt.setString(2, reservation.guestName());
            stmt.setString(3, reservation.guestEmail());
            stmt.setString(4, reservation.guestCpf());
            stmt.setDate(5, Date.valueOf(reservation.checkIn()));
            stmt.setDate(6, Date.valueOf(reservation.checkOut()));
            stmt.setInt(7, reservation.guests());
            stmt.setString(8, reservation.ratePlan().name());
            stmt.setInt(9, reservation.nights());
            stmt.setBigDecimal(10, reservation.subtotal());
            stmt.setBigDecimal(11, reservation.discount());
            stmt.setBigDecimal(12, reservation.total());
            stmt.setString(13, reservation.status().name());
            stmt.setBigDecimal(14, reservation.amountPaid());
            if (reservation.refund() != null) {
                stmt.setBigDecimal(15, reservation.refund());
            } else {
                stmt.setNull(15, java.sql.Types.DECIMAL);
            }

            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return new Reservation(id, reservation.roomNumber(), reservation.guestName(),
                        reservation.guestEmail(), reservation.guestCpf(), reservation.checkIn(),
                        reservation.checkOut(), reservation.guests(), reservation.ratePlan(),
                        reservation.nights(), reservation.subtotal(), reservation.discount(),
                        reservation.total(), reservation.status(), reservation.amountPaid(),
                        reservation.refund());
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }

        throw new RuntimeException("Failed to get generated id");
    }

    private void update(Reservation reservation) {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
        """;

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.status().name());
            stmt.setBigDecimal(2, reservation.amountPaid());
            if (reservation.refund() != null) {
                stmt.setBigDecimal(3, reservation.refund());
            } else {
                stmt.setNull(3, java.sql.Types.DECIMAL);
            }
            stmt.setLong(4, reservation.id());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (var rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Reservation(
                        rs.getLong("id"),
                        rs.getString("room_number"),
                        rs.getString("guest_name"),
                        rs.getString("guest_email"),
                        rs.getString("guest_cpf"),
                        rs.getDate("check_in").toLocalDate(),
                        rs.getDate("check_out").toLocalDate(),
                        rs.getInt("guests"),
                        RatePlan.valueOf(rs.getString("rate_plan")),
                        rs.getInt("nights"),
                        rs.getBigDecimal("subtotal"),
                        rs.getBigDecimal("discount"),
                        rs.getBigDecimal("total"),
                        ReservationStatus.valueOf(rs.getString("status")),
                        rs.getBigDecimal("amount_paid"),
                        rs.getBigDecimal("refund")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservation", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Reservation> findActiveByRoomAndDateRange(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
            AND status != 'CANCELLED'
            AND check_in < ?
            AND check_out > ?
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(new Reservation(
                        rs.getLong("id"),
                        rs.getString("room_number"),
                        rs.getString("guest_name"),
                        rs.getString("guest_email"),
                        rs.getString("guest_cpf"),
                        rs.getDate("check_in").toLocalDate(),
                        rs.getDate("check_out").toLocalDate(),
                        rs.getInt("guests"),
                        RatePlan.valueOf(rs.getString("rate_plan")),
                        rs.getInt("nights"),
                        rs.getBigDecimal("subtotal"),
                        rs.getBigDecimal("discount"),
                        rs.getBigDecimal("total"),
                        ReservationStatus.valueOf(rs.getString("status")),
                        rs.getBigDecimal("amount_paid"),
                        rs.getBigDecimal("refund")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations", e);
        }

        return reservations;
    }

    @Override
    public List<Reservation> findPaidByDate(LocalDate date) {
        String sql = """
            SELECT * FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ?
            AND check_out > ?
            ORDER BY room_number
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(new Reservation(
                        rs.getLong("id"),
                        rs.getString("room_number"),
                        rs.getString("guest_name"),
                        rs.getString("guest_email"),
                        rs.getString("guest_cpf"),
                        rs.getDate("check_in").toLocalDate(),
                        rs.getDate("check_out").toLocalDate(),
                        rs.getInt("guests"),
                        RatePlan.valueOf(rs.getString("rate_plan")),
                        rs.getInt("nights"),
                        rs.getBigDecimal("subtotal"),
                        rs.getBigDecimal("discount"),
                        rs.getBigDecimal("total"),
                        ReservationStatus.valueOf(rs.getString("status")),
                        rs.getBigDecimal("amount_paid"),
                        rs.getBigDecimal("refund")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations by date", e);
        }

        return reservations;
    }

    @Override
    public List<Reservation> findByCheckInMonth(YearMonth month) {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
            AND EXTRACT(MONTH FROM check_in) = ?
            ORDER BY check_in, id
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, month.getYear());
            stmt.setInt(2, month.getMonthValue());

            try (var rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(new Reservation(
                        rs.getLong("id"),
                        rs.getString("room_number"),
                        rs.getString("guest_name"),
                        rs.getString("guest_email"),
                        rs.getString("guest_cpf"),
                        rs.getDate("check_in").toLocalDate(),
                        rs.getDate("check_out").toLocalDate(),
                        rs.getInt("guests"),
                        RatePlan.valueOf(rs.getString("rate_plan")),
                        rs.getInt("nights"),
                        rs.getBigDecimal("subtotal"),
                        rs.getBigDecimal("discount"),
                        rs.getBigDecimal("total"),
                        ReservationStatus.valueOf(rs.getString("status")),
                        rs.getBigDecimal("amount_paid"),
                        rs.getBigDecimal("refund")
                    ));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations by month", e);
        }

        return reservations;
    }
}
