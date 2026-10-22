package com.pousadamaralta.infrastructure.repository;

import com.pousadamaralta.domain.identification.CnpjIdentification;
import com.pousadamaralta.domain.identification.CpfIdentification;
import com.pousadamaralta.domain.identification.GuestIdentification;
import com.pousadamaralta.domain.identification.PassportIdentification;
import com.pousadamaralta.domain.model.RatePlanType;
import com.pousadamaralta.domain.model.Reservation;
import com.pousadamaralta.domain.model.ReservationStatus;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
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

    public Reservation save(Reservation reservation) {
        if (reservation.getId() == null) {
            return insert(reservation);
        } else {
            return update(reservation);
        }
    }

    private Reservation insert(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (
                room_number, guest_name, guest_email, guest_cpf, company_cnpj, company_name,
                guest_passport, guest_country, check_in, check_out, guests, rate_plan_type,
                nights, subtotal, discount, total, status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setReservationParameters(stmt, reservation);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return new Reservation(
                        id,
                        reservation.getRoomNumber(),
                        reservation.getGuestName(),
                        reservation.getGuestEmail(),
                        reservation.getIdentification(),
                        reservation.getCheckIn(),
                        reservation.getCheckOut(),
                        reservation.getGuests(),
                        reservation.getRatePlanType(),
                        reservation.getNights(),
                        reservation.getSubtotal(),
                        reservation.getDiscount(),
                        reservation.getTotal(),
                        reservation.getStatus(),
                        reservation.getAmountPaid(),
                        reservation.getRefund()
                    );
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }

        throw new RuntimeException("Failed to get generated ID");
    }

    private Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET
                room_number = ?, guest_name = ?, guest_email = ?, guest_cpf = ?, company_cnpj = ?,
                company_name = ?, guest_passport = ?, guest_country = ?, check_in = ?, check_out = ?,
                guests = ?, rate_plan_type = ?, nights = ?, subtotal = ?, discount = ?, total = ?,
                status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
        """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);
            stmt.setLong(20, reservation.getId());
            stmt.executeUpdate();

            return reservation;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation reservation) throws Exception {
        GuestIdentification id = reservation.getIdentification();

        stmt.setString(1, reservation.getRoomNumber());
        stmt.setString(2, reservation.getGuestName());
        stmt.setString(3, reservation.getGuestEmail());
        stmt.setString(4, id.getCpf());
        stmt.setString(5, id.getCnpj());
        stmt.setString(6, id.getCompanyName());
        stmt.setString(7, id.getPassport());
        stmt.setString(8, id.getCountry());
        stmt.setDate(9, Date.valueOf(reservation.getCheckIn()));
        stmt.setDate(10, Date.valueOf(reservation.getCheckOut()));
        stmt.setInt(11, reservation.getGuests());
        stmt.setString(12, reservation.getRatePlanType().name());
        stmt.setInt(13, reservation.getNights());
        stmt.setBigDecimal(14, reservation.getSubtotal());
        stmt.setBigDecimal(15, reservation.getDiscount());
        stmt.setBigDecimal(16, reservation.getTotal());
        stmt.setString(17, reservation.getStatus().name());
        stmt.setBigDecimal(18, reservation.getAmountPaid());

        if (reservation.getRefund() != null) {
            stmt.setBigDecimal(19, reservation.getRefund());
        } else {
            stmt.setNull(19, java.sql.Types.DECIMAL);
        }
    }

    public Optional<Reservation> findById(Long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch reservation", e);
        }

        return Optional.empty();
    }

    public List<Reservation> findByRoomAndDateRange(String roomNumber, LocalDate start, LocalDate end) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
              AND check_out > ?
              AND check_in < ?
              AND status != 'CANCELLED'
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(start));
            stmt.setDate(3, Date.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch reservations", e);
        }

        return reservations;
    }

    public List<Reservation> findPaidReservationsForNight(LocalDate night) {
        String sql = """
            SELECT * FROM reservations
            WHERE check_in <= ?
              AND check_out > ?
              AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            ORDER BY room_number
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch reservations for night", e);
        }

        return reservations;
    }

    public List<Reservation> findRevenueReservationsForMonth(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth().plusDays(1);

        String sql = """
            SELECT * FROM reservations
            WHERE check_in >= ?
              AND check_in < ?
              AND (amount_paid > 0 OR refund IS NOT NULL)
            ORDER BY check_in, id
        """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch revenue reservations", e);
        }

        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws Exception {
        GuestIdentification identification = mapIdentification(rs);

        BigDecimal refund = rs.getBigDecimal("refund");
        if (rs.wasNull()) {
            refund = null;
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
            RatePlanType.valueOf(rs.getString("rate_plan_type")),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            ReservationStatus.valueOf(rs.getString("status")),
            rs.getBigDecimal("amount_paid"),
            refund
        );
    }

    private GuestIdentification mapIdentification(ResultSet rs) throws Exception {
        String cpf = rs.getString("guest_cpf");
        if (cpf != null) {
            return new CpfIdentification(cpf);
        }

        String cnpj = rs.getString("company_cnpj");
        if (cnpj != null) {
            String companyName = rs.getString("company_name");
            return new CnpjIdentification(cnpj, companyName);
        }

        String passport = rs.getString("guest_passport");
        if (passport != null) {
            String country = rs.getString("guest_country");
            return new PassportIdentification(passport, country);
        }

        throw new IllegalStateException("Invalid guest identification data");
    }
}
