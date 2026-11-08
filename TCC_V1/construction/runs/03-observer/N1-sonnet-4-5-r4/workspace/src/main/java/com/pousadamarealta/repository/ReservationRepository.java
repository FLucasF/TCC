package com.pousadamarealta.repository;

import com.pousadamarealta.domain.model.RatePlan;
import com.pousadamarealta.domain.model.Reservation;
import com.pousadamarealta.domain.model.ReservationStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
    private final Connection connection;

    public ReservationRepository(Connection connection) {
        this.connection = connection;
    }

    public void initialize() throws SQLException {
        String createTable = """
            CREATE TABLE IF NOT EXISTS reservations (
                id SERIAL PRIMARY KEY,
                room_number VARCHAR(10) NOT NULL,
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(14) NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(20) NOT NULL,
                status VARCHAR(20) NOT NULL,
                amount_paid DECIMAL(10, 2) NOT NULL,
                refund DECIMAL(10, 2)
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTable);
        }
    }

    public Reservation save(Reservation reservation) throws SQLException {
        if (reservation.getId() == null) {
            return insert(reservation);
        } else {
            update(reservation);
            return reservation;
        }
    }

    private Reservation insert(Reservation reservation) throws SQLException {
        String sql = """
            INSERT INTO reservations
            (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, status, amount_paid, refund)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, reservation);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    reservation.setId(rs.getLong("id"));
                }
            }
        }

        return reservation;
    }

    private void update(Reservation reservation) throws SQLException {
        String sql = """
            UPDATE reservations
            SET room_number = ?, guest_name = ?, guest_email = ?, guest_cpf = ?,
                check_in = ?, check_out = ?, guests = ?, rate_plan = ?, status = ?,
                amount_paid = ?, refund = ?
            WHERE id = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParameters(stmt, reservation);
            stmt.setLong(12, reservation.getId());
            stmt.executeUpdate();
        }
    }

    private void setParameters(PreparedStatement stmt, Reservation reservation) throws SQLException {
        stmt.setString(1, reservation.getRoomNumber());
        stmt.setString(2, reservation.getGuestName());
        stmt.setString(3, reservation.getGuestEmail());
        stmt.setString(4, reservation.getGuestCpf());
        stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
        stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
        stmt.setInt(7, reservation.getGuests());
        stmt.setString(8, reservation.getRatePlan().name());
        stmt.setString(9, reservation.getStatus().name());
        stmt.setBigDecimal(10, reservation.getAmountPaid());
        if (reservation.getRefund() != null) {
            stmt.setBigDecimal(11, reservation.getRefund());
        } else {
            stmt.setNull(11, Types.DECIMAL);
        }
    }

    public Optional<Reservation> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }

    public List<Reservation> findConflicting(String roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = """
            SELECT * FROM reservations
            WHERE room_number = ?
            AND status != 'CANCELLED'
            AND check_in < ?
            AND check_out > ?
        """;

        List<Reservation> reservations = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        }

        return reservations;
    }

    public List<Reservation> findByNight(LocalDate night) throws SQLException {
        String sql = """
            SELECT * FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ?
            AND check_out > ?
            ORDER BY room_number
        """;

        List<Reservation> reservations = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(night));
            stmt.setDate(2, Date.valueOf(night));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        }

        return reservations;
    }

    public List<Reservation> findByMonth(YearMonth month) throws SQLException {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth().plusDays(1);

        String sql = """
            SELECT * FROM reservations
            WHERE check_in >= ? AND check_in < ?
            ORDER BY check_in, id
        """;

        List<Reservation> reservations = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapRow(rs));
                }
            }
        }

        return reservations;
    }

    private Reservation mapRow(ResultSet rs) throws SQLException {
        Reservation reservation = new Reservation(
            rs.getString("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlan.valueOf(rs.getString("rate_plan"))
        );

        reservation.setId(rs.getLong("id"));

        String status = rs.getString("status");
        BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
        BigDecimal refund = rs.getBigDecimal("refund");

        if (status.equals(ReservationStatus.CONFIRMED.name())) {
            reservation.confirmWithTotal(amountPaid);
        } else if (status.equals(ReservationStatus.CHECKED_IN.name())) {
            reservation.confirmWithTotal(amountPaid);
            reservation.checkIn();
        } else if (status.equals(ReservationStatus.CHECKED_OUT.name())) {
            reservation.confirmWithTotal(amountPaid);
            reservation.checkIn();
            reservation.checkOut();
        } else if (status.equals(ReservationStatus.CANCELLED.name())) {
            if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
                reservation.confirmWithTotal(amountPaid);
            }
            reservation.cancel();
        }

        return reservation;
    }
}
