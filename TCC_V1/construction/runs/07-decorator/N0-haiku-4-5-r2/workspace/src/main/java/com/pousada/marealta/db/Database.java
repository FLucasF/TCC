package com.pousada.marealta.db;

import com.pousada.marealta.models.*;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class Database {
    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public Database(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public void initialize() throws SQLException {
        try (Connection conn = getConnection()) {
            createTables(conn);
            initializeRooms(conn);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rooms (
                    number INTEGER PRIMARY KEY,
                    type VARCHAR(50) NOT NULL,
                    capacity INTEGER NOT NULL,
                    nightly_rate DECIMAL(10, 2) NOT NULL
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id BIGSERIAL PRIMARY KEY,
                    room_number INTEGER NOT NULL REFERENCES rooms(number),
                    guest_name VARCHAR(255) NOT NULL,
                    guest_email VARCHAR(255) NOT NULL,
                    guest_cpf VARCHAR(14) NOT NULL,
                    check_in DATE NOT NULL,
                    check_out DATE NOT NULL,
                    guests INTEGER NOT NULL,
                    rate_plan VARCHAR(50) NOT NULL,
                    nights INTEGER NOT NULL,
                    subtotal DECIMAL(10, 2) NOT NULL,
                    discount DECIMAL(10, 2) NOT NULL,
                    total DECIMAL(10, 2) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    amount_paid DECIMAL(10, 2) NOT NULL DEFAULT 0,
                    refund DECIMAL(10, 2)
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id BIGSERIAL PRIMARY KEY,
                    channel VARCHAR(50) NOT NULL,
                    to_address VARCHAR(255) NOT NULL,
                    subject VARCHAR(255) NOT NULL,
                    body TEXT NOT NULL,
                    status VARCHAR(50) NOT NULL
                )
                """);
        }
    }

    private void initializeRooms(Connection conn) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING")) {

            Room[] rooms = {
                new Room(101, "STANDARD", 2, new BigDecimal("249.90")),
                new Room(102, "STANDARD", 2, new BigDecimal("249.90")),
                new Room(201, "DELUXE", 3, new BigDecimal("389.90")),
                new Room(202, "DELUXE", 3, new BigDecimal("389.90")),
                new Room(301, "SUITE", 4, new BigDecimal("579.90"))
            };

            for (Room room : rooms) {
                pstmt.setInt(1, room.number);
                pstmt.setString(2, room.type);
                pstmt.setInt(3, room.capacity);
                pstmt.setBigDecimal(4, room.nightlyRate);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public Room getRoom(int roomNumber) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?")) {
            pstmt.setInt(1, roomNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Room(rs.getInt("number"), rs.getString("type"),
                            rs.getInt("capacity"), rs.getBigDecimal("nightly_rate"));
                }
            }
        }
        return null;
    }

    public List<Room> getAllRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number")) {
            while (rs.next()) {
                rooms.add(new Room(rs.getInt("number"), rs.getString("type"),
                        rs.getInt("capacity"), rs.getBigDecimal("nightly_rate")));
            }
        }
        return rooms;
    }

    public long createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                   LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
                                   int nights, BigDecimal subtotal, BigDecimal discount, BigDecimal total) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                     Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, roomNumber);
            pstmt.setString(2, guestName);
            pstmt.setString(3, guestEmail);
            pstmt.setString(4, guestCpf);
            pstmt.setDate(5, Date.valueOf(checkIn));
            pstmt.setDate(6, Date.valueOf(checkOut));
            pstmt.setInt(7, guests);
            pstmt.setString(8, ratePlan);
            pstmt.setInt(9, nights);
            pstmt.setBigDecimal(10, subtotal);
            pstmt.setBigDecimal(11, discount);
            pstmt.setBigDecimal(12, total);
            pstmt.setString(13, "PENDING");
            pstmt.setBigDecimal(14, BigDecimal.ZERO);

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    public Reservation getReservation(long id) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund FROM reservations WHERE id = ?")) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        }
        return null;
    }

    public void updateReservationStatus(long id, ReservationStatus status, BigDecimal amountPaid, BigDecimal refund) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?")) {
            pstmt.setString(1, status.name());
            pstmt.setBigDecimal(2, amountPaid);
            pstmt.setBigDecimal(3, refund);
            pstmt.setLong(4, id);
            pstmt.executeUpdate();
        }
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut, long excludeReservationId) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM reservations WHERE room_number = ? AND status != 'CANCELLED' AND id != ? AND check_in < ? AND check_out > ?")) {
            pstmt.setInt(1, roomNumber);
            pstmt.setLong(2, excludeReservationId);
            pstmt.setDate(3, Date.valueOf(checkOut));
            pstmt.setDate(4, Date.valueOf(checkIn));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0;
                }
            }
        }
        return true;
    }

    public long createMessage(String channel, String to, String subject, String body, MessageStatus status) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO messages (channel, to_address, subject, body, status) VALUES (?, ?, ?, ?, ?) RETURNING id",
                     Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, channel);
            pstmt.setString(2, to);
            pstmt.setString(3, subject);
            pstmt.setString(4, body);
            pstmt.setString(5, status.name());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    public List<Message> getAllMessages() throws SQLException {
        List<Message> messages = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, channel, to_address, subject, body, status FROM messages ORDER BY id")) {
            while (rs.next()) {
                messages.add(new Message(
                        rs.getLong("id"),
                        rs.getString("channel"),
                        rs.getString("to_address"),
                        rs.getString("subject"),
                        rs.getString("body"),
                        MessageStatus.valueOf(rs.getString("status"))
                ));
            }
        }
        return messages;
    }

    public List<String[]> getOccupancyReport(LocalDate date) throws SQLException {
        List<String[]> report = new ArrayList<>();
        try (Connection conn = getConnection()) {
            String query = """
                SELECT r.number, r.type, res.id, res.check_in, res.check_out
                FROM rooms r
                LEFT JOIN reservations res ON r.number = res.room_number
                    AND res.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                    AND res.check_in <= ? AND res.check_out > ?
                ORDER BY r.number
                """;
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setDate(1, Date.valueOf(date));
                pstmt.setDate(2, Date.valueOf(date));
                try (ResultSet rs = pstmt.executeQuery()) {
                    int occupiedCount = 0;
                    while (rs.next()) {
                        int roomNumber = rs.getInt("number");
                        String roomType = rs.getString("type");
                        Long reservationId = rs.getObject("id") != null ? rs.getLong("id") : null;

                        if (reservationId != null) {
                            LocalDate checkIn = rs.getDate("check_in").toLocalDate();
                            LocalDate checkOut = rs.getDate("check_out").toLocalDate();
                            report.add(new String[]{
                                    String.valueOf(roomNumber),
                                    roomType,
                                    String.valueOf(reservationId),
                                    checkIn.toString(),
                                    checkOut.toString()
                            });
                            occupiedCount++;
                        }
                    }
                    report.add(new String[]{"total", String.valueOf(occupiedCount) + "/5"});
                }
            }
        }
        return report;
    }

    public List<String[]> getRevenueReport(int year, int month) throws SQLException {
        List<String[]> report = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        try (Connection conn = getConnection()) {
            String query = """
                SELECT id, room_number, check_in, check_out, status, amount_paid, refund
                FROM reservations
                WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ?
                    AND (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT' OR status = 'CANCELLED')
                ORDER BY check_in, id
                """;
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, year);
                pstmt.setInt(2, month);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
                        BigDecimal refund = rs.getBigDecimal("refund");
                        BigDecimal netAmount = amountPaid.subtract(refund != null ? refund : BigDecimal.ZERO);

                        report.add(new String[]{
                                String.valueOf(rs.getLong("id")),
                                String.valueOf(rs.getInt("room_number")),
                                rs.getDate("check_in").toString(),
                                rs.getDate("check_out").toString(),
                                rs.getString("status"),
                                netAmount.toPlainString()
                        });
                        total = total.add(netAmount);
                    }
                }
            }
        }
        report.add(new String[]{"total", total.toPlainString()});
        return report;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        BigDecimal refund = rs.getBigDecimal("refund");
        return new Reservation(
                rs.getLong("id"),
                rs.getInt("room_number"),
                rs.getString("guest_name"),
                rs.getString("guest_email"),
                rs.getString("guest_cpf"),
                rs.getDate("check_in").toLocalDate(),
                rs.getDate("check_out").toLocalDate(),
                rs.getInt("guests"),
                rs.getString("rate_plan"),
                rs.getInt("nights"),
                rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount"),
                rs.getBigDecimal("total"),
                ReservationStatus.valueOf(rs.getString("status")),
                rs.getBigDecimal("amount_paid"),
                refund
        );
    }
}
