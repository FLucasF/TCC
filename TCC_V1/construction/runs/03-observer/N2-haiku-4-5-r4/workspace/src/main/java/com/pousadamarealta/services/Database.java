package com.pousadamarealta.services;

import com.pousadamarealta.domain.*;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

public class Database {
    private static final Logger logger = Logger.getLogger(Database.class.getName());
    private final HikariDataSource dataSource;

    public Database(String dbUrl, String dbUser, String dbPassword) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        this.dataSource = new HikariDataSource(config);
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS rooms (
                        number INTEGER PRIMARY KEY,
                        type VARCHAR(20) NOT NULL,
                        capacity INTEGER NOT NULL,
                        nightly_rate NUMERIC(10, 2) NOT NULL
                    )
                    """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS reservations (
                        id BIGSERIAL PRIMARY KEY,
                        room_number INTEGER NOT NULL,
                        guest_name VARCHAR(255) NOT NULL,
                        guest_email VARCHAR(255) NOT NULL,
                        guest_cpf VARCHAR(14) NOT NULL,
                        check_in DATE NOT NULL,
                        check_out DATE NOT NULL,
                        guests INTEGER NOT NULL,
                        rate_plan VARCHAR(20) NOT NULL,
                        nights INTEGER NOT NULL,
                        subtotal NUMERIC(10, 2) NOT NULL,
                        discount NUMERIC(10, 2) NOT NULL,
                        total NUMERIC(10, 2) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
                        refund NUMERIC(10, 2),
                        FOREIGN KEY (room_number) REFERENCES rooms(number)
                    )
                    """);

                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS messages (
                        id BIGSERIAL PRIMARY KEY,
                        channel VARCHAR(20) NOT NULL,
                        "to" VARCHAR(255) NOT NULL,
                        subject VARCHAR(255) NOT NULL,
                        body TEXT NOT NULL
                    )
                    """);

                // Insert predefined rooms
                try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) as count FROM rooms")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt("count") == 0) {
                            insertPredefinedRooms(conn);
                        }
                    }
                }
            }

            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void insertPredefinedRooms(Connection conn) throws SQLException {
        String sql = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, 101);
            ps.setString(2, "STANDARD");
            ps.setInt(3, 2);
            ps.setBigDecimal(4, new BigDecimal("249.90"));
            ps.addBatch();

            ps.setInt(1, 102);
            ps.setString(2, "STANDARD");
            ps.setInt(3, 2);
            ps.setBigDecimal(4, new BigDecimal("249.90"));
            ps.addBatch();

            ps.setInt(1, 201);
            ps.setString(2, "DELUXE");
            ps.setInt(3, 3);
            ps.setBigDecimal(4, new BigDecimal("389.90"));
            ps.addBatch();

            ps.setInt(1, 202);
            ps.setString(2, "DELUXE");
            ps.setInt(3, 3);
            ps.setBigDecimal(4, new BigDecimal("389.90"));
            ps.addBatch();

            ps.setInt(1, 301);
            ps.setString(2, "SUITE");
            ps.setInt(3, 4);
            ps.setBigDecimal(4, new BigDecimal("579.90"));
            ps.addBatch();

            ps.executeBatch();
        }
    }

    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    RoomType.fromValue(rs.getString("type")),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch rooms", e);
        }

        return rooms;
    }

    public Optional<Room> getRoomByNumber(int number) {
        String sql = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Room(
                        rs.getInt("number"),
                        RoomType.fromValue(rs.getString("type")),
                        rs.getInt("capacity"),
                        rs.getBigDecimal("nightly_rate")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch room", e);
        }

        return Optional.empty();
    }

    public long insertReservation(Reservation reservation) {
        String sql = """
            INSERT INTO reservations
            (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
             rate_plan, nights, subtotal, discount, total, status, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, reservation.getRoomNumber());
            ps.setString(2, reservation.getGuestName());
            ps.setString(3, reservation.getGuestEmail());
            ps.setString(4, reservation.getGuestCpf());
            ps.setDate(5, java.sql.Date.valueOf(reservation.getCheckIn()));
            ps.setDate(6, java.sql.Date.valueOf(reservation.getCheckOut()));
            ps.setInt(7, reservation.getGuests());
            ps.setString(8, reservation.getRatePlan().getValue());
            ps.setInt(9, reservation.getNights());
            ps.setBigDecimal(10, reservation.getSubtotal());
            ps.setBigDecimal(11, reservation.getDiscount());
            ps.setBigDecimal(12, reservation.getTotal());
            ps.setString(13, reservation.getStatus().getValue());
            ps.setBigDecimal(14, reservation.getAmountPaid());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert reservation", e);
        }

        throw new RuntimeException("Failed to insert reservation");
    }

    public Optional<Reservation> getReservationById(long id) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(resultSetToReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch reservation", e);
        }

        return Optional.empty();
    }

    public void updateReservation(Reservation reservation) {
        String sql = """
            UPDATE reservations
            SET status = ?, amount_paid = ?, refund = ?
            WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, reservation.getStatus().getValue());
            ps.setBigDecimal(2, reservation.getAmountPaid());
            ps.setBigDecimal(3, reservation.getRefund());
            ps.setLong(4, reservation.getId());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation", e);
        }
    }

    public void insertMessage(Message message) {
        String sql = """
            INSERT INTO messages (channel, "to", subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, message.getChannel().getValue());
            ps.setString(2, message.getTo());
            ps.setString(3, message.getSubject());
            ps.setString(4, message.getBody());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert message", e);
        }
    }

    public List<Message> getAllMessages() {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, \"to\", subject, body FROM messages ORDER BY id";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    MessageChannel.valueOf(rs.getString("channel")),
                    rs.getString("to"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch messages", e);
        }

        return messages;
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT COUNT(*) as count FROM reservations
            WHERE room_number = ?
            AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND NOT (check_out <= ? OR check_in >= ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, roomNumber);
            ps.setDate(2, java.sql.Date.valueOf(checkIn));
            ps.setDate(3, java.sql.Date.valueOf(checkOut));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") == 0;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check room availability", e);
        }

        return false;
    }

    public List<Reservation> getReservationsForOccupancyReport(LocalDate date) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ? AND check_out > ?
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(date));
            ps.setDate(2, java.sql.Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(resultSetToReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch occupancy report", e);
        }

        return reservations;
    }

    public List<Reservation> getReservationsForRevenueReport(int year, int month) {
        String sql = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ?
            AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')
            AND amount_paid > 0
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, year);
            ps.setInt(2, month);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(resultSetToReservation(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch revenue report", e);
        }

        return reservations;
    }

    private Reservation resultSetToReservation(ResultSet rs) throws SQLException {
        Reservation res = new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            RatePlan.fromValue(rs.getString("rate_plan")),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total")
        );
        res.setStatus(ReservationStatus.fromValue(rs.getString("status")));
        res.setAmountPaid(rs.getBigDecimal("amount_paid"));
        res.setRefund(rs.getBigDecimal("refund"));
        return res;
    }

    public void close() {
        dataSource.close();
    }
}
