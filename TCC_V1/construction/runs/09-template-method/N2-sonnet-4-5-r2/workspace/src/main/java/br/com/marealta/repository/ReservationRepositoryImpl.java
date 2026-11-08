package br.com.marealta.repository;

import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.ReservationStatus;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepositoryImpl implements ReservationRepository {
    private final DataSource dataSource;

    public ReservationRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Reservation save(Reservation reservation) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     """
                     INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                         check_in, check_out, guests, rate_plan, nights, subtotal, discount, total,
                         status, amount_paid, refund)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """,
                     Statement.RETURN_GENERATED_KEYS)) {
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
            stmt.setBigDecimal(14, reservation.getAmountPaid());
            stmt.setBigDecimal(15, reservation.getRefund());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                rs.next();
                long id = rs.getLong(1);
                return new Reservation(
                        id,
                        reservation.getRoomNumber(),
                        reservation.getGuestName(),
                        reservation.getGuestEmail(),
                        reservation.getGuestCpf(),
                        reservation.getCheckIn(),
                        reservation.getCheckOut(),
                        reservation.getGuests(),
                        reservation.getRatePlan(),
                        reservation.getNights(),
                        reservation.getSubtotal(),
                        reservation.getDiscount(),
                        reservation.getTotal(),
                        reservation.getStatus(),
                        reservation.getAmountPaid(),
                        reservation.getRefund()
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation", e);
        }
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM reservations WHERE id = ?")) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation by id", e);
        }
    }

    @Override
    public void update(Reservation reservation) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     """
                     UPDATE reservations
                     SET status = ?, amount_paid = ?, refund = ?
                     WHERE id = ?
                     """)) {
            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            stmt.setBigDecimal(3, reservation.getRefund());
            stmt.setLong(4, reservation.getId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    @Override
    public List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     """
                     SELECT * FROM reservations
                     WHERE room_number = ?
                       AND status != 'CANCELLED'
                       AND check_in < ?
                       AND check_out > ?
                     """)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = stmt.executeQuery()) {
                List<Reservation> reservations = new ArrayList<>();
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
                return reservations;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by room and date range", e);
        }
    }

    @Override
    public List<Reservation> findByStatusIn(List<ReservationStatus> statuses) {
        String placeholders = String.join(",", statuses.stream().map(s -> "?").toList());
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM reservations WHERE status IN (" + placeholders + ")")) {
            for (int i = 0; i < statuses.size(); i++) {
                stmt.setString(i + 1, statuses.get(i).name());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<Reservation> reservations = new ArrayList<>();
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
                return reservations;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by status", e);
        }
    }

    @Override
    public List<Reservation> findPaidByDate(LocalDate date) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     """
                     SELECT * FROM reservations
                     WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                       AND check_in <= ?
                       AND check_out > ?
                     ORDER BY room_number
                     """)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                List<Reservation> reservations = new ArrayList<>();
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
                return reservations;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find paid reservations by date", e);
        }
    }

    @Override
    public List<Reservation> findByCheckInMonth(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth().plusDays(1);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     """
                     SELECT * FROM reservations
                     WHERE check_in >= ? AND check_in < ?
                     ORDER BY check_in, id
                     """)) {
            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));
            try (ResultSet rs = stmt.executeQuery()) {
                List<Reservation> reservations = new ArrayList<>();
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
                return reservations;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by check-in month", e);
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getLong("id"),
                rs.getInt("room_number"),
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
        );
    }
}
