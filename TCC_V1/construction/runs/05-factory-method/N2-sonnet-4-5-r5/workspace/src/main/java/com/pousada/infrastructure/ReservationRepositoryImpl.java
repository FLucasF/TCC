package com.pousada.infrastructure;

import com.pousada.domain.guest.GuestType;
import com.pousada.domain.guest.GuestTypeFactory;
import com.pousada.domain.model.RatePlan;
import com.pousada.domain.model.Reservation;
import com.pousada.domain.model.ReservationStatus;
import com.pousada.domain.repository.ReservationRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ReservationRepositoryImpl implements ReservationRepository {
    private final DataSource dataSource;

    public ReservationRepositoryImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Reservation save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (room_number, room_type, guest_name, guest_email, guest_cpf, company_cnpj,
                company_name, guest_passport, guest_country, check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setReservationParameters(stmt, reservation);

            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                reservation.setId(keys.getLong(1));
            }

            return reservation;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation", e);
        }
    }

    @Override
    public void update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, reservation.status().name());
            stmt.setBigDecimal(2, reservation.amountPaid());

            if (reservation.refund() != null) {
                stmt.setBigDecimal(3, reservation.refund());
            } else {
                stmt.setNull(3, Types.DECIMAL);
            }

            stmt.setLong(4, reservation.id());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    @Override
    public Optional<Reservation> findById(long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapReservation(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Reservation> findByRoomAndDateRange(String roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
            AND check_in < ?
            AND check_out > ?
            AND status != 'CANCELLED'
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations", e);
        }

        return reservations;
    }

    @Override
    public List<Reservation> findPaidByNight(LocalDate night) {
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

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by night", e);
        }

        return reservations;
    }

    @Override
    public List<Reservation> findByCheckInMonth(int year, int month) {
        String sql = """
            SELECT * FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ?
            AND EXTRACT(MONTH FROM check_in) = ?
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, year);
            stmt.setInt(2, month);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by month", e);
        }

        return reservations;
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation r) throws SQLException {
        stmt.setString(1, r.roomNumber());
        stmt.setString(2, r.roomType().name());
        stmt.setString(3, r.guestName());
        stmt.setString(4, r.guestEmail());

        GuestType gt = r.guestType();
        stmt.setString(5, gt.guestCpf());
        stmt.setString(6, gt.companyCnpj());
        stmt.setString(7, gt.companyName());
        stmt.setString(8, gt.guestPassport());
        stmt.setString(9, gt.guestCountry());

        stmt.setDate(10, Date.valueOf(r.checkIn()));
        stmt.setDate(11, Date.valueOf(r.checkOut()));
        stmt.setInt(12, r.guests());
        stmt.setString(13, r.ratePlan().name());
        stmt.setInt(14, r.nights());
        stmt.setBigDecimal(15, r.subtotal());
        stmt.setBigDecimal(16, r.discount());
        stmt.setBigDecimal(17, r.total());
        stmt.setString(18, r.status().name());
        stmt.setBigDecimal(19, r.amountPaid());

        if (r.refund() != null) {
            stmt.setBigDecimal(20, r.refund());
        } else {
            stmt.setNull(20, Types.DECIMAL);
        }
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        GuestType guestType = GuestTypeFactory.create(
            rs.getString("guest_cpf"),
            rs.getString("company_cnpj"),
            rs.getString("company_name"),
            rs.getString("guest_passport"),
            rs.getString("guest_country")
        );

        Reservation.Builder builder = Reservation.builder()
            .id(rs.getLong("id"))
            .roomNumber(rs.getString("room_number"))
            .roomType(com.pousada.domain.model.RoomType.valueOf(rs.getString("room_type")))
            .guestName(rs.getString("guest_name"))
            .guestEmail(rs.getString("guest_email"))
            .guestType(guestType)
            .checkIn(rs.getDate("check_in").toLocalDate())
            .checkOut(rs.getDate("check_out").toLocalDate())
            .guests(rs.getInt("guests"))
            .ratePlan(RatePlan.valueOf(rs.getString("rate_plan")))
            .nights(rs.getInt("nights"))
            .subtotal(rs.getBigDecimal("subtotal"))
            .discount(rs.getBigDecimal("discount"))
            .total(rs.getBigDecimal("total"))
            .status(ReservationStatus.valueOf(rs.getString("status")))
            .amountPaid(rs.getBigDecimal("amount_paid"));

        var refund = rs.getBigDecimal("refund");
        if (refund != null) {
            builder.refund(refund);
        }

        return builder.build();
    }
}
