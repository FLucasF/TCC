package com.pousada.marealta.infrastructure;

import com.pousada.marealta.domain.RatePlan;
import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.ReservationStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final Database database;

    public ReservationRepository(Database database) {
        this.database = database;
    }

    public Reservation create(Reservation reservation) throws SQLException {
        String sql = """
                INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf,
                    check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status)
                VALUES (%d, '%s', '%s', '%s', '%s', '%s', %d, '%s', %d, %s, %s, %s, '%s')
                """.formatted(
                reservation.getRoomNumber(),
                escape(reservation.getGuestName()),
                escape(reservation.getGuestEmail()),
                escape(reservation.getGuestCpf()),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getGuests(),
                reservation.getRatePlan().name(),
                reservation.getNights(),
                reservation.getSubtotal(),
                reservation.getDiscount(),
                reservation.getTotal(),
                reservation.getStatus().name());

        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                reservation.setId(rs.getLong(1));
            }
        }
        return reservation;
    }

    public Optional<Reservation> findById(Long id) throws SQLException {
        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM reservations WHERE id = " + id)) {
            if (rs.next()) {
                return Optional.of(mapReservation(rs));
            }
        }
        return Optional.empty();
    }

    public void update(Reservation reservation) throws SQLException {
        String sql = """
                UPDATE reservations SET
                    status = '%s',
                    amount_paid = %s,
                    refund = %s
                WHERE id = %d
                """.formatted(
                reservation.getStatus().name(),
                reservation.getAmountPaid(),
                reservation.getRefund() != null ? reservation.getRefund().toString() : "NULL",
                reservation.getId());

        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = """
                SELECT COUNT(*) as count FROM reservations
                WHERE room_number = %d
                AND status != 'CANCELLED'
                AND check_in < '%s' AND check_out > '%s'
                """.formatted(roomNumber, checkOut, checkIn);

        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("count") == 0;
            }
        }
        return true;
    }

    public List<Reservation> findByDate(LocalDate date) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = """
                SELECT * FROM reservations
                WHERE status != 'CANCELLED'
                AND check_in <= '%s' AND check_out > '%s'
                ORDER BY room_number
                """.formatted(date, date);

        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        }
        return reservations;
    }

    public List<Reservation> findByMonth(int year, int month) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        String sql = """
                SELECT * FROM reservations
                WHERE (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT')
                AND check_in >= '%s' AND check_in <= '%s'
                ORDER BY check_in, id
                """.formatted(startDate, endDate);

        try (Connection conn = database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reservations.add(mapReservation(rs));
            }
        }
        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getLong("id"));
        r.setRoomNumber(rs.getInt("room_number"));
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

    private String escape(String value) {
        return value.replace("'", "''");
    }
}
