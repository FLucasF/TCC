package br.com.marealta.repository;

import br.com.marealta.domain.*;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final DataSource dataSource;

    public ReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initSchema() {
        String createTable = """
            CREATE TABLE IF NOT EXISTS reservations (
                id SERIAL PRIMARY KEY,
                room_number VARCHAR(10) NOT NULL,
                room_type VARCHAR(20) NOT NULL,
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(14) NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(50) NOT NULL,
                company_code VARCHAR(50),
                status VARCHAR(20) NOT NULL,
                amount_paid DECIMAL(10, 2) NOT NULL,
                refund DECIMAL(10, 2)
            )
            """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTable);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create reservations table", e);
        }
    }

    public Reservation save(Reservation reservation) {
        String insert = """
            INSERT INTO reservations
            (room_number, room_type, guest_name, guest_email, guest_cpf,
             check_in, check_out, guests, rate_plan, company_code, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getRoomType().name());
            stmt.setString(3, reservation.getGuestName());
            stmt.setString(4, reservation.getGuestEmail());
            stmt.setString(5, reservation.getGuestCpf());
            stmt.setDate(6, Date.valueOf(reservation.getDateRange().checkIn()));
            stmt.setDate(7, Date.valueOf(reservation.getDateRange().checkOut()));
            stmt.setInt(8, reservation.getGuests());
            stmt.setString(9, reservation.getRatePlanType());
            stmt.setString(10, reservation.getCompanyCode());
            stmt.setString(11, reservation.getStatus().name());
            stmt.setBigDecimal(12, reservation.getAmountPaid().amount());
            stmt.setBigDecimal(13, reservation.getRefund() != null ? reservation.getRefund().amount() : null);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation", e);
        }
    }

    public void update(Reservation reservation) {
        String update = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(update)) {

            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid().amount());
            stmt.setBigDecimal(3, reservation.getRefund() != null ? reservation.getRefund().amount() : null);
            stmt.setLong(4, reservation.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String query = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, company_code, status, amount_paid, refund
            FROM reservations
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation", e);
        }

        return Optional.empty();
    }

    public List<Reservation> findConflicting(String roomNumber, DateRange dateRange, Long excludeId) {
        String query = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, company_code, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
              AND check_in < ?
              AND check_out > ?
              AND status != 'CANCELLED'
              AND id != ?
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(dateRange.checkOut()));
            stmt.setDate(3, Date.valueOf(dateRange.checkIn()));
            stmt.setLong(4, excludeId != null ? excludeId : -1);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find conflicting reservations", e);
        }

        return reservations;
    }

    public List<Reservation> findByNight(LocalDate night) {
        String query = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, company_code, status, amount_paid, refund
            FROM reservations
            WHERE check_in <= ?
              AND check_out > ?
              AND (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT')
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by night", e);
        }

        return reservations;
    }

    public List<Reservation> findByMonth(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth().plusDays(1);

        String query = """
            SELECT id, room_number, room_type, guest_name, guest_email, guest_cpf,
                   check_in, check_out, guests, rate_plan, company_code, status, amount_paid, refund
            FROM reservations
            WHERE check_in >= ? AND check_in < ?
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by month", e);
        }

        return reservations;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String roomNumber = rs.getString("room_number");
        RoomType roomType = RoomType.valueOf(rs.getString("room_type"));
        String guestName = rs.getString("guest_name");
        String guestEmail = rs.getString("guest_email");
        String guestCpf = rs.getString("guest_cpf");
        LocalDate checkIn = rs.getDate("check_in").toLocalDate();
        LocalDate checkOut = rs.getDate("check_out").toLocalDate();
        int guests = rs.getInt("guests");
        String ratePlan = rs.getString("rate_plan");
        String companyCode = rs.getString("company_code");
        ReservationStatus status = ReservationStatus.valueOf(rs.getString("status"));
        Money amountPaid = new Money(rs.getBigDecimal("amount_paid"));
        Money refund = rs.getBigDecimal("refund") != null ? new Money(rs.getBigDecimal("refund")) : null;

        DateRange dateRange = new DateRange(checkIn, checkOut);
        Reservation reservation = new Reservation(id, roomNumber, roomType, guestName, guestEmail,
                                                  guestCpf, dateRange, guests, ratePlan, companyCode);
        reservation.setStatus(status);
        reservation.setAmountPaid(amountPaid);
        reservation.setRefund(refund);

        return reservation;
    }
}
