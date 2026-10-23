package br.com.pousada.repository;

import br.com.pousada.domain.*;

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
        initializeTable();
    }

    private void initializeTable() {
        try (Connection conn = dataSource.getConnection()) {
            String createTable = """
                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    room_number VARCHAR(10) NOT NULL,
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(11),
                    company_cnpj VARCHAR(14),
                    company_name VARCHAR(255),
                    guest_passport VARCHAR(50),
                    guest_country VARCHAR(100),
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(20) NOT NULL,
                    nights INTEGER NOT NULL,
                    subtotal DECIMAL(10, 2) NOT NULL,
                    discount DECIMAL(10, 2) NOT NULL,
                    total DECIMAL(10, 2) NOT NULL,
                    status VARCHAR(20) NOT NULL,
                    amount_paid DECIMAL(10, 2),
                    refund DECIMAL(10, 2)
                )
                """;

            try (PreparedStatement stmt = conn.prepareStatement(createTable)) {
                stmt.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize reservations table", e);
        }
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
                company_cnpj, company_name, guest_passport, guest_country,
                check_in, check_out, guests, rate_plan, nights, subtotal, discount, total,
                status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setReservationParameters(stmt, reservation);
            stmt.execute();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong(1));
                }
            }

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }
    }

    private Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.getStatus().name());
            stmt.setBigDecimal(2, reservation.getAmountPaid());
            stmt.setBigDecimal(3, reservation.getRefund());
            stmt.setLong(4, reservation.getId());
            stmt.execute();

            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation reservation) throws SQLException {
        stmt.setString(1, reservation.getRoomNumber());
        stmt.setString(2, reservation.getGuestName());
        stmt.setString(3, reservation.getGuestEmail());

        GuestIdentification id = reservation.getIdentification();
        switch (id) {
            case CpfIdentification cpf -> {
                stmt.setString(4, cpf.cpf());
                stmt.setNull(5, Types.VARCHAR);
                stmt.setNull(6, Types.VARCHAR);
                stmt.setNull(7, Types.VARCHAR);
                stmt.setNull(8, Types.VARCHAR);
            }
            case CnpjIdentification cnpj -> {
                stmt.setNull(4, Types.VARCHAR);
                stmt.setString(5, cnpj.cnpj());
                stmt.setString(6, cnpj.companyName());
                stmt.setNull(7, Types.VARCHAR);
                stmt.setNull(8, Types.VARCHAR);
            }
            case PassportIdentification passport -> {
                stmt.setNull(4, Types.VARCHAR);
                stmt.setNull(5, Types.VARCHAR);
                stmt.setNull(6, Types.VARCHAR);
                stmt.setString(7, passport.passport());
                stmt.setString(8, passport.country());
            }
        }

        stmt.setDate(9, Date.valueOf(reservation.getCheckIn()));
        stmt.setDate(10, Date.valueOf(reservation.getCheckOut()));
        stmt.setInt(11, reservation.getGuests());
        stmt.setString(12, reservation.getRatePlan().name());
        stmt.setInt(13, reservation.getNights());
        stmt.setBigDecimal(14, reservation.getSubtotal());
        stmt.setBigDecimal(15, reservation.getDiscount());
        stmt.setBigDecimal(16, reservation.getTotal());
        stmt.setString(17, reservation.getStatus().name());
        stmt.setBigDecimal(18, reservation.getAmountPaid());
        stmt.setBigDecimal(19, reservation.getRefund());
    }

    public Optional<Reservation> findById(Long id) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                company_name, guest_passport, guest_country, check_in, check_out, guests,
                rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservation", e);
        }

        return Optional.empty();
    }

    public List<Reservation> findActiveByRoomAndDateRange(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                company_name, guest_passport, guest_country, check_in, check_out, guests,
                rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ?
                AND status != 'CANCELLED'
                AND check_in < ?
                AND check_out > ?
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations", e);
        }

        return reservations;
    }

    public List<Reservation> findPaidByDate(LocalDate date) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                company_name, guest_passport, guest_country, check_in, check_out, guests,
                rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT')
                AND check_in <= ?
                AND check_out > ?
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations", e);
        }

        return reservations;
    }

    public List<Reservation> findByMonth(int year, int month) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, company_cnpj,
                company_name, guest_passport, guest_country, check_in, check_out, guests,
                rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
                AND EXTRACT(MONTH FROM check_in) = ?
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservations", e);
        }

        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        GuestIdentification identification;
        String cpf = rs.getString("guest_cpf");
        String cnpj = rs.getString("company_cnpj");
        String passport = rs.getString("guest_passport");

        if (cpf != null) {
            identification = new CpfIdentification(cpf);
        } else if (cnpj != null) {
            identification = new CnpjIdentification(cnpj, rs.getString("company_name"));
        } else {
            identification = new PassportIdentification(passport, rs.getString("guest_country"));
        }

        return new Reservation(
            rs.getLong("id"),
            rs.getString("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            identification,
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
