package com.pousadamarealta.repository;

import com.pousadamarealta.domain.*;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReservationRepository {
    private final DataSource dataSource;

    public ReservationRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initialize() {
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS reservations (" +
                "id SERIAL PRIMARY KEY, " +
                "room_number INTEGER NOT NULL, " +
                "guest_name VARCHAR(255) NOT NULL, " +
                "guest_email VARCHAR(255) NOT NULL, " +
                "guest_cpf VARCHAR(14), " +
                "guest_passport VARCHAR(50), " +
                "guest_country VARCHAR(100), " +
                "company_cnpj VARCHAR(18), " +
                "company_name VARCHAR(255), " +
                "check_in DATE NOT NULL, " +
                "check_out DATE NOT NULL, " +
                "guests INTEGER NOT NULL, " +
                "rate_plan VARCHAR(20) NOT NULL, " +
                "nights INTEGER NOT NULL, " +
                "subtotal DECIMAL(10,2) NOT NULL, " +
                "discount DECIMAL(10,2) NOT NULL, " +
                "total DECIMAL(10,2) NOT NULL, " +
                "status VARCHAR(20) NOT NULL, " +
                "amount_paid DECIMAL(10,2) NOT NULL, " +
                "refund DECIMAL(10,2))"
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize reservations table", e);
        }
    }

    public Reservation save(Reservation reservation) {
        if (reservation.id() == 0) {
            return insert(reservation);
        } else {
            update(reservation);
            return reservation;
        }
    }

    private Reservation insert(Reservation reservation) {
        String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, " +
                    "guest_cpf, guest_passport, guest_country, company_cnpj, company_name, " +
                    "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, " +
                    "status, amount_paid, refund) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setReservationParameters(stmt, reservation);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    return new Reservation(id, reservation.roomNumber(), reservation.guestName(),
                        reservation.guestEmail(), reservation.guestIdentification(),
                        reservation.checkIn(), reservation.checkOut(), reservation.guests(),
                        reservation.ratePlan(), reservation.nights(), reservation.subtotal(),
                        reservation.discount(), reservation.total(), reservation.status(),
                        reservation.amountPaid(), reservation.refund());
                }
                throw new SQLException("Failed to get generated ID");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }
    }

    private void update(Reservation reservation) {
        String sql = "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservation.status().name());
            stmt.setBigDecimal(2, reservation.amountPaid());
            stmt.setBigDecimal(3, reservation.refund());
            stmt.setLong(4, reservation.id());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation r) throws SQLException {
        stmt.setInt(1, r.roomNumber());
        stmt.setString(2, r.guestName());
        stmt.setString(3, r.guestEmail());

        GuestIdentification guestId = r.guestIdentification();
        if (guestId instanceof GuestIdentification.IndividualGuest individual) {
            stmt.setString(4, individual.cpf());
            stmt.setNull(5, Types.VARCHAR);
            stmt.setNull(6, Types.VARCHAR);
            stmt.setNull(7, Types.VARCHAR);
            stmt.setNull(8, Types.VARCHAR);
        } else if (guestId instanceof GuestIdentification.ForeignGuest foreign) {
            stmt.setNull(4, Types.VARCHAR);
            stmt.setString(5, foreign.passport());
            stmt.setString(6, foreign.country());
            stmt.setNull(7, Types.VARCHAR);
            stmt.setNull(8, Types.VARCHAR);
        } else if (guestId instanceof GuestIdentification.CorporateGuest corporate) {
            stmt.setNull(4, Types.VARCHAR);
            stmt.setNull(5, Types.VARCHAR);
            stmt.setNull(6, Types.VARCHAR);
            stmt.setString(7, corporate.cnpj());
            stmt.setString(8, corporate.companyName());
        }

        stmt.setDate(9, Date.valueOf(r.checkIn()));
        stmt.setDate(10, Date.valueOf(r.checkOut()));
        stmt.setInt(11, r.guests());
        stmt.setString(12, r.ratePlan().name());
        stmt.setInt(13, r.nights());
        stmt.setBigDecimal(14, r.subtotal());
        stmt.setBigDecimal(15, r.discount());
        stmt.setBigDecimal(16, r.total());
        stmt.setString(17, r.status().name());
        stmt.setBigDecimal(18, r.amountPaid());
        stmt.setBigDecimal(19, r.refund());
    }

    public Optional<Reservation> findById(long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation", e);
        }
    }

    public List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = "SELECT * FROM reservations WHERE room_number = ? " +
                    "AND status != 'CANCELLED' " +
                    "AND check_in < ? AND check_out > ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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
            throw new RuntimeException("Failed to find reservations", e);
        }
    }

    public List<Reservation> findByCheckInMonth(int year, int month) {
        String sql = "SELECT * FROM reservations WHERE EXTRACT(YEAR FROM check_in) = ? " +
                    "AND EXTRACT(MONTH FROM check_in) = ? ORDER BY check_in, id";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Reservation> reservations = new ArrayList<>();
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
                return reservations;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by month", e);
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        GuestIdentification guestId;
        String cpf = rs.getString("guest_cpf");
        String passport = rs.getString("guest_passport");
        String cnpj = rs.getString("company_cnpj");

        if (cpf != null) {
            guestId = new GuestIdentification.IndividualGuest(cpf);
        } else if (passport != null) {
            guestId = new GuestIdentification.ForeignGuest(
                passport, rs.getString("guest_country"));
        } else {
            guestId = new GuestIdentification.CorporateGuest(
                cnpj, rs.getString("company_name"));
        }

        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            guestId,
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
