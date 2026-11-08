package com.pousada.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.pousada.model.*;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class Database {
    private final HikariDataSource dataSource;

    public Database(String dbUrl, String dbUser, String dbPassword) throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);

        this.dataSource = new HikariDataSource(config);
        initializeDatabase();
    }

    private void initializeDatabase() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            createTablesIfNeeded(conn);
            insertRoomsIfNeeded(conn);
        }
    }

    private void createTablesIfNeeded(Connection conn) throws SQLException {
        String createRoomsTable = """
            CREATE TABLE IF NOT EXISTS rooms (
                number INTEGER PRIMARY KEY,
                type VARCHAR(50) NOT NULL,
                capacity INTEGER NOT NULL,
                nightly_rate NUMERIC(10, 2) NOT NULL
            )
            """;

        String createReservationsTable = """
            CREATE TABLE IF NOT EXISTS reservations (
                id BIGSERIAL PRIMARY KEY,
                room_number INTEGER NOT NULL REFERENCES rooms(number),
                guest_name VARCHAR(255) NOT NULL,
                guest_email VARCHAR(255) NOT NULL,
                guest_cpf VARCHAR(20) NOT NULL,
                check_in DATE NOT NULL,
                check_out DATE NOT NULL,
                guests INTEGER NOT NULL,
                rate_plan VARCHAR(50) NOT NULL,
                nights INTEGER NOT NULL,
                subtotal NUMERIC(10, 2) NOT NULL,
                discount NUMERIC(10, 2) NOT NULL,
                total NUMERIC(10, 2) NOT NULL,
                status VARCHAR(50) NOT NULL,
                amount_paid NUMERIC(10, 2) NOT NULL DEFAULT 0,
                refund NUMERIC(10, 2),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createMessagesTable = """
            CREATE TABLE IF NOT EXISTS messages (
                id BIGSERIAL PRIMARY KEY,
                channel VARCHAR(50) NOT NULL,
                to_address VARCHAR(255) NOT NULL,
                subject VARCHAR(255) NOT NULL,
                body TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createRoomsTable);
            stmt.execute(createReservationsTable);
            stmt.execute(createMessagesTable);
        }
    }

    private void insertRoomsIfNeeded(Connection conn) throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM rooms";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(checkQuery);
            rs.next();
            if (rs.getInt(1) == 0) {
                insertDefaultRooms(conn);
            }
        }
    }

    private void insertDefaultRooms(Connection conn) throws SQLException {
        String insertQuery = "INSERT INTO rooms (number, type, capacity, nightly_rate) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
            stmt.setInt(1, 101);
            stmt.setString(2, "STANDARD");
            stmt.setInt(3, 2);
            stmt.setBigDecimal(4, new BigDecimal("249.90"));
            stmt.addBatch();

            stmt.setInt(1, 102);
            stmt.setString(2, "STANDARD");
            stmt.setInt(3, 2);
            stmt.setBigDecimal(4, new BigDecimal("249.90"));
            stmt.addBatch();

            stmt.setInt(1, 201);
            stmt.setString(2, "DELUXE");
            stmt.setInt(3, 3);
            stmt.setBigDecimal(4, new BigDecimal("389.90"));
            stmt.addBatch();

            stmt.setInt(1, 202);
            stmt.setString(2, "DELUXE");
            stmt.setInt(3, 3);
            stmt.setBigDecimal(4, new BigDecimal("389.90"));
            stmt.addBatch();

            stmt.setInt(1, 301);
            stmt.setString(2, "SUITE");
            stmt.setInt(3, 4);
            stmt.setBigDecimal(4, new BigDecimal("579.90"));
            stmt.addBatch();

            stmt.executeBatch();
        }
    }

    public List<Room> getAllRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String query = "SELECT number, type, capacity, nightly_rate FROM rooms ORDER BY number";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                rooms.add(new Room(
                    rs.getInt("number"),
                    RoomType.valueOf(rs.getString("type")),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                ));
            }
        }

        return rooms;
    }

    public Room getRoomByNumber(int roomNumber) throws SQLException {
        String query = "SELECT number, type, capacity, nightly_rate FROM rooms WHERE number = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, roomNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Room(
                    rs.getInt("number"),
                    RoomType.valueOf(rs.getString("type")),
                    rs.getInt("capacity"),
                    rs.getBigDecimal("nightly_rate")
                );
            }
        }

        return null;
    }

    public Reservation createReservation(Reservation reservation) throws SQLException {
        String insertQuery = """
            INSERT INTO reservations
            (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan,
             nights, subtotal, discount, total, status, amount_paid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, reservation.getRoomNumber());
            stmt.setString(2, reservation.getGuestName());
            stmt.setString(3, reservation.getGuestEmail());
            stmt.setString(4, reservation.getGuestCpf());
            stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(7, reservation.getGuests());
            stmt.setString(8, reservation.getRatePlan().name());
            stmt.setInt(9, reservation.getNights());
            stmt.setBigDecimal(10, reservation.getSubtotal());
            stmt.setBigDecimal(11, reservation.getDiscount());
            stmt.setBigDecimal(12, reservation.getTotal());
            stmt.setString(13, ReservationStatus.PENDING.name());
            stmt.setBigDecimal(14, BigDecimal.ZERO);

            stmt.executeUpdate();

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                reservation.setId(generatedKeys.getLong(1));
            }
            reservation.setStatus(ReservationStatus.PENDING);
            reservation.setAmountPaid(BigDecimal.ZERO);

            return reservation;
        }
    }

    public Reservation getReservationById(long id) throws SQLException {
        String query = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations WHERE id = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToReservation(rs);
            }
        }

        return null;
    }

    public List<Reservation> getReservationsByRoomAndDates(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String query = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE room_number = ? AND status != 'CANCELLED'
            AND check_in < ? AND check_out > ?
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, roomNumber);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        }

        return reservations;
    }

    public void updateReservationStatus(long id, ReservationStatus status) throws SQLException {
        String query = "UPDATE reservations SET status = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, status.name());
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    public void updateReservationAfterConfirmation(long id, BigDecimal amountPaid) throws SQLException {
        String query = "UPDATE reservations SET status = ?, amount_paid = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, ReservationStatus.CONFIRMED.name());
            stmt.setBigDecimal(2, amountPaid);
            stmt.setLong(3, id);
            stmt.executeUpdate();
        }
    }

    public void updateReservationAfterCancellation(long id, BigDecimal refund) throws SQLException {
        String query = "UPDATE reservations SET status = ?, refund = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, ReservationStatus.CANCELLED.name());
            stmt.setBigDecimal(2, refund);
            stmt.setLong(3, id);
            stmt.executeUpdate();
        }
    }

    public long saveMessage(MessageChannel channel, String to, String subject, String body) throws SQLException {
        String insertQuery = """
            INSERT INTO messages (channel, to_address, subject, body)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, channel.name());
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, body);

            stmt.executeUpdate();

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getLong(1);
            }
        }

        return -1;
    }

    public List<Message> getAllMessages() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String query = """
            SELECT id, channel, to_address, subject, body
            FROM messages ORDER BY id
            """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                messages.add(new Message(
                    rs.getLong("id"),
                    MessageChannel.valueOf(rs.getString("channel")),
                    rs.getString("to_address"),
                    rs.getString("subject"),
                    rs.getString("body")
                ));
            }
        }

        return messages;
    }

    public List<Reservation> getReservationsForOccupancy(LocalDate date) throws SQLException {
        String query = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
            AND check_in <= ? AND check_out > ?
            ORDER BY room_number
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        }

        return reservations;
    }

    public List<Reservation> getReservationsForMonth(int year, int month) throws SQLException {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);

        String query = """
            SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out,
                   guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund
            FROM reservations
            WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED')
            AND amount_paid > 0
            AND check_in >= ? AND check_in < ?
            ORDER BY check_in, id
            """;

        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setDate(1, Date.valueOf(startDate));
            stmt.setDate(2, Date.valueOf(endDate));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                reservations.add(mapResultSetToReservation(rs));
            }
        }

        return reservations;
    }

    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
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

    public void close() throws SQLException {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
