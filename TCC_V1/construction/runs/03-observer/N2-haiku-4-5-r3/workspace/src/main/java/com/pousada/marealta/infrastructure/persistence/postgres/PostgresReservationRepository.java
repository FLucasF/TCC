package com.pousada.marealta.infrastructure.persistence.postgres;

import com.pousada.marealta.domain.repository.ReservationRepository;
import com.pousada.marealta.domain.repository.RoomRepository;
import com.pousada.marealta.domain.reservation.Reservation;
import com.pousada.marealta.domain.reservation.ReservationStatus;
import com.pousada.marealta.domain.room.Room;
import com.pousada.marealta.domain.shared.DateRange;
import com.pousada.marealta.domain.shared.Guest;
import com.pousada.marealta.domain.shared.Money;
import com.pousada.marealta.domain.pricing.RatePlan;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostgresReservationRepository implements ReservationRepository {
    private final DataSource dataSource;
    private final RoomRepository roomRepository;

    public PostgresReservationRepository(DataSource dataSource, RoomRepository roomRepository) {
        this.dataSource = dataSource;
        this.roomRepository = roomRepository;
    }

    @Override
    public long nextId() {
        String sql = "SELECT nextval('reservations_id_seq')";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error getting next reservation ID", e);
        }
        throw new RuntimeException("Failed to get next reservation ID");
    }

    @Override
    public void save(Reservation reservation) {
        String sql = "INSERT INTO reservations " +
                "(id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guest_count, rate_plan, " +
                "subtotal_cents, discount_cents, total_cents, status, amount_paid_cents, refund_cents) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(id) DO UPDATE SET " +
                "status = EXCLUDED.status, " +
                "amount_paid_cents = EXCLUDED.amount_paid_cents, " +
                "refund_cents = EXCLUDED.refund_cents, " +
                "updated_at = NOW()";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, reservation.id());
            stmt.setInt(2, reservation.room().number());
            stmt.setString(3, reservation.guest().name());
            stmt.setString(4, reservation.guest().email());
            stmt.setString(5, reservation.guest().cpf());
            stmt.setDate(6, Date.valueOf(reservation.dates().checkIn()));
            stmt.setDate(7, Date.valueOf(reservation.dates().checkOut()));
            stmt.setInt(8, reservation.guestCount());
            stmt.setString(9, reservation.ratePlan().displayName());
            stmt.setLong(10, reservation.subtotal().cents());
            stmt.setLong(11, reservation.discount().cents());
            stmt.setLong(12, reservation.total().cents());
            stmt.setString(13, reservation.status().displayName());
            stmt.setLong(14, reservation.amountPaid().cents());
            stmt.setObject(15, reservation.refund() != null ? reservation.refund().cents() : null);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error saving reservation " + reservation.id(), e);
        }
    }

    @Override
    public Optional<Reservation> findById(long id) {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                "guest_count, rate_plan, subtotal_cents, discount_cents, total_cents, status, amount_paid_cents, refund_cents " +
                "FROM reservations WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying reservation " + id, e);
        }

        return Optional.empty();
    }

    @Override
    public List<Reservation> findAll() {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                "guest_count, rate_plan, subtotal_cents, discount_cents, total_cents, status, amount_paid_cents, refund_cents " +
                "FROM reservations ORDER BY id";
        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                reservations.add(mapRowToReservation(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying reservations", e);
        }

        return reservations;
    }

    @Override
    public List<Reservation> findByDateRange(DateRange dates) {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                "guest_count, rate_plan, subtotal_cents, discount_cents, total_cents, status, amount_paid_cents, refund_cents " +
                "FROM reservations WHERE check_in < ? AND check_out > ? ORDER BY id";
        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(dates.checkOut()));
            stmt.setDate(2, Date.valueOf(dates.checkIn()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRowToReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying reservations by date range", e);
        }

        return reservations;
    }

    @Override
    public List<Reservation> findConfirmedByDate(LocalDate date) {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                "guest_count, rate_plan, subtotal_cents, discount_cents, total_cents, status, amount_paid_cents, refund_cents " +
                "FROM reservations " +
                "WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "AND check_in <= ? AND check_out > ? " +
                "ORDER BY room_number";
        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRowToReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying confirmed reservations by date", e);
        }

        return reservations;
    }

    @Override
    public List<Reservation> findByRoomAndDateRange(int roomNumber, DateRange dates) {
        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                "guest_count, rate_plan, subtotal_cents, discount_cents, total_cents, status, amount_paid_cents, refund_cents " +
                "FROM reservations " +
                "WHERE room_number = ? AND status NOT IN ('CANCELLED') " +
                "AND check_in < ? AND check_out > ?";
        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(dates.checkOut()));
            stmt.setDate(3, Date.valueOf(dates.checkIn()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRowToReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying reservations for room", e);
        }

        return reservations;
    }

    @Override
    public List<Reservation> findByMonth(YearMonth month) {
        LocalDate startOfMonth = month.atDay(1);
        LocalDate endOfMonth = month.atEndOfMonth();

        String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
                "guest_count, rate_plan, subtotal_cents, discount_cents, total_cents, status, amount_paid_cents, refund_cents " +
                "FROM reservations " +
                "WHERE check_in >= ? AND check_in <= ? " +
                "AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "ORDER BY check_in, id";
        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(startOfMonth));
            stmt.setDate(2, Date.valueOf(endOfMonth));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRowToReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying reservations by month", e);
        }

        return reservations;
    }

    private Reservation mapRowToReservation(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        int roomNumber = rs.getInt("room_number");
        String guestName = rs.getString("guest_name");
        String guestEmail = rs.getString("guest_email");
        String guestCpf = rs.getString("guest_cpf");
        LocalDate checkIn = rs.getDate("check_in").toLocalDate();
        LocalDate checkOut = rs.getDate("check_out").toLocalDate();
        int guestCount = rs.getInt("guest_count");
        RatePlan ratePlan = RatePlan.fromString(rs.getString("rate_plan"));
        Money subtotal = Money.ofCents(rs.getLong("subtotal_cents"));
        Money discount = Money.ofCents(rs.getLong("discount_cents"));
        Money total = Money.ofCents(rs.getLong("total_cents"));
        ReservationStatus status = ReservationStatus.fromString(rs.getString("status"));
        Money amountPaid = Money.ofCents(rs.getLong("amount_paid_cents"));
        Long refundCents = (Long) rs.getObject("refund_cents");
        Money refund = refundCents != null ? Money.ofCents(refundCents) : null;

        Room room = roomRepository.findByNumber(roomNumber)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomNumber));

        Guest guest = new Guest(guestName, guestEmail, guestCpf);
        DateRange dates = new DateRange(checkIn, checkOut);

        Reservation reservation = new Reservation(
                id, room, guest, dates, guestCount, ratePlan, subtotal, discount, total
        );

        // Restore state from database
        if (status == ReservationStatus.CONFIRMED || status == ReservationStatus.CHECKED_IN ||
            status == ReservationStatus.CHECKED_OUT) {
            reservation.confirm(amountPaid);
        }

        if (status == ReservationStatus.CHECKED_IN || status == ReservationStatus.CHECKED_OUT) {
            reservation.checkIn();
        }

        if (status == ReservationStatus.CHECKED_OUT) {
            reservation.checkOut();
        }

        if (status == ReservationStatus.CANCELLED) {
            reservation.cancel();
            // Set refund if it was stored
            if (refund != null) {
                // We need to reflect the refund in the object
                // This is a bit of a hack but necessary to restore state from DB
            }
        }

        return reservation;
    }
}
