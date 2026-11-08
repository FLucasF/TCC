package br.com.pousadamarealta.repository.postgres;

import br.com.pousadamarealta.domain.*;
import br.com.pousadamarealta.repository.ReservationRepository;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class PostgresReservationRepository implements ReservationRepository {
    private final Database database;

    public PostgresReservationRepository(Database database) {
        this.database = database;
    }

    @Override
    public Reservation save(Reservation reservation) {
        if (reservation.id() == null) {
            return insert(reservation);
        } else {
            return update(reservation);
        }
    }

    private Reservation insert(Reservation reservation) {
        String sql = """
            INSERT INTO reservations (
                room_number, room_type, room_capacity, room_nightly_rate,
                guest_name, guest_email, guest_cpf,
                check_in, check_out, guests, rate_plan,
                nights, subtotal, discount, total,
                status, amount_paid, refund
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    return new Reservation(
                        id,
                        reservation.roomNumber(),
                        reservation.roomType(),
                        reservation.roomCapacity(),
                        reservation.roomNightlyRate(),
                        reservation.guestName(),
                        reservation.guestEmail(),
                        reservation.guestCpf(),
                        reservation.checkIn(),
                        reservation.checkOut(),
                        reservation.guests(),
                        reservation.ratePlan(),
                        reservation.nights(),
                        reservation.subtotal(),
                        reservation.discount(),
                        reservation.total(),
                        reservation.status(),
                        reservation.amountPaid(),
                        reservation.refund()
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }

        throw new RuntimeException("Failed to get generated ID");
    }

    private Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations SET
                room_number = ?, room_type = ?, room_capacity = ?, room_nightly_rate = ?,
                guest_name = ?, guest_email = ?, guest_cpf = ?,
                check_in = ?, check_out = ?, guests = ?, rate_plan = ?,
                nights = ?, subtotal = ?, discount = ?, total = ?,
                status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setReservationParameters(stmt, reservation);
            stmt.setLong(19, reservation.id());

            stmt.executeUpdate();
            return reservation;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    private void setReservationParameters(PreparedStatement stmt, Reservation reservation) throws SQLException {
        stmt.setString(1, reservation.roomNumber());
        stmt.setString(2, reservation.roomType().name());
        stmt.setInt(3, reservation.roomCapacity());
        stmt.setBigDecimal(4, reservation.roomNightlyRate());
        stmt.setString(5, reservation.guestName());
        stmt.setString(6, reservation.guestEmail());
        stmt.setString(7, reservation.guestCpf());
        stmt.setDate(8, Date.valueOf(reservation.checkIn()));
        stmt.setDate(9, Date.valueOf(reservation.checkOut()));
        stmt.setInt(10, reservation.guests());
        stmt.setString(11, reservation.ratePlan().name());
        stmt.setInt(12, reservation.nights());
        stmt.setBigDecimal(13, reservation.subtotal());
        stmt.setBigDecimal(14, reservation.discount());
        stmt.setBigDecimal(15, reservation.total());
        stmt.setString(16, reservation.status().name());
        stmt.setBigDecimal(17, reservation.amountPaid());
        if (reservation.refund() != null) {
            stmt.setBigDecimal(18, reservation.refund());
        } else {
            stmt.setNull(18, Types.DECIMAL);
        }
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation by id", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Reservation> findByRoomAndNight(String roomNumber, LocalDate night, ReservationStatus... excludeStatuses) {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
            AND check_in <= ?
            AND check_out > ?
            """;

        if (excludeStatuses.length > 0) {
            String placeholders = Arrays.stream(excludeStatuses)
                .map(s -> "?")
                .collect(Collectors.joining(", "));
            sql += " AND status NOT IN (" + placeholders + ")";
        }

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(night));
            stmt.setDate(3, Date.valueOf(night));

            for (int i = 0; i < excludeStatuses.length; i++) {
                stmt.setString(4 + i, excludeStatuses[i].name());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by room and night", e);
        }

        return reservations;
    }

    @Override
    public List<Reservation> findByNight(LocalDate night) {
        String sql = """
            SELECT * FROM reservations
            WHERE check_in <= ?
            AND check_out > ?
            AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by night", e);
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

        try (Connection conn = database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, month.getYear());
            stmt.setInt(2, month.getMonthValue());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by check-in month", e);
        }

        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getLong("id"),
            rs.getString("room_number"),
            RoomType.valueOf(rs.getString("room_type")),
            rs.getInt("room_capacity"),
            rs.getBigDecimal("room_nightly_rate"),
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
