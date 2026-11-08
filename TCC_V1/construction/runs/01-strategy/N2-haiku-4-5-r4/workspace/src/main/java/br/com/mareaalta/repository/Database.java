package br.com.mareaalta.repository;

import br.com.mareaalta.domain.*;
import br.com.mareaalta.util.DateUtils;
import br.com.mareaalta.util.Money;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class Database {
    private String jdbcUrl;
    private String user;
    private String password;

    public Database(String jdbcUrl, String user, String password) {
        this.jdbcUrl = jdbcUrl;
        this.user = user;
        this.password = password;
    }

    public void initialize() throws SQLException {
        try (Connection conn = getConnection()) {
            createTables(conn);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        String[] tables = {
                "CREATE TABLE IF NOT EXISTS rooms (" +
                        "number INT PRIMARY KEY," +
                        "type VARCHAR(20) NOT NULL" +
                        ")",

                "CREATE TABLE IF NOT EXISTS reservations (" +
                        "id BIGSERIAL PRIMARY KEY," +
                        "room_number INT NOT NULL REFERENCES rooms(number)," +
                        "guest_name VARCHAR(255) NOT NULL," +
                        "guest_email VARCHAR(255) NOT NULL," +
                        "guest_cpf VARCHAR(20) NOT NULL," +
                        "check_in DATE NOT NULL," +
                        "check_out DATE NOT NULL," +
                        "guests INT NOT NULL," +
                        "rate_plan VARCHAR(50) NOT NULL," +
                        "company_code VARCHAR(50)," +
                        "nights INT NOT NULL," +
                        "subtotal NUMERIC(10,2) NOT NULL," +
                        "discount NUMERIC(10,2) NOT NULL," +
                        "total NUMERIC(10,2) NOT NULL," +
                        "status VARCHAR(20) NOT NULL," +
                        "amount_paid NUMERIC(10,2) NOT NULL DEFAULT 0," +
                        "refund NUMERIC(10,2)," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")",

                "CREATE TABLE IF NOT EXISTS messages (" +
                        "id BIGSERIAL PRIMARY KEY," +
                        "channel VARCHAR(50) NOT NULL," +
                        "\"to\" VARCHAR(255) NOT NULL," +
                        "subject VARCHAR(500) NOT NULL," +
                        "body TEXT NOT NULL," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")"
        };

        try (Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                stmt.execute(table);
            }
        }

        insertDefaultRooms(conn);
    }

    private void insertDefaultRooms(Connection conn) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM rooms";
        try (Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(checkSql)) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    String insertSql = "INSERT INTO rooms (number, type) VALUES (?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        ps.setInt(1, 101);
                        ps.setString(2, "STANDARD");
                        ps.addBatch();

                        ps.setInt(1, 102);
                        ps.setString(2, "STANDARD");
                        ps.addBatch();

                        ps.setInt(1, 201);
                        ps.setString(2, "DELUXE");
                        ps.addBatch();

                        ps.setInt(1, 202);
                        ps.setString(2, "DELUXE");
                        ps.addBatch();

                        ps.setInt(1, 301);
                        ps.setString(2, "SUITE");
                        ps.addBatch();

                        ps.executeBatch();
                    }
                }
            }
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

    public List<Room> getAllRooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT number, type FROM rooms ORDER BY number";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int number = rs.getInt("number");
                RoomType type = RoomType.fromString(rs.getString("type"));
                rooms.add(new Room(number, type));
            }
        }
        return rooms;
    }

    public Room getRoomByNumber(int number) throws SQLException {
        String sql = "SELECT number, type FROM rooms WHERE number = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    RoomType type = RoomType.fromString(rs.getString("type"));
                    return new Room(number, type);
                }
            }
        }
        return null;
    }

    public Long createReservation(Reservation res) throws SQLException {
        String sql = "INSERT INTO reservations " +
                "(room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, " +
                "rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "RETURNING id";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, res.getRoomNumber());
            ps.setString(2, res.getGuestName());
            ps.setString(3, res.getGuestEmail());
            ps.setString(4, res.getGuestCpf());
            ps.setObject(5, res.getCheckIn());
            ps.setObject(6, res.getCheckOut());
            ps.setInt(7, res.getGuests());
            ps.setString(8, res.getRatePlan().toString());
            ps.setString(9, res.getCompanyCode());
            ps.setInt(10, res.getNights());
            ps.setDouble(11, res.getSubtotal());
            ps.setDouble(12, res.getDiscount());
            ps.setDouble(13, res.getTotal());
            ps.setString(14, res.getStatus().toString());
            ps.setDouble(15, res.getAmountPaid());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        return null;
    }

    public Reservation getReservationById(Long id) throws SQLException {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapReservation(rs);
                }
            }
        }
        return null;
    }

    public void updateReservation(Reservation res) throws SQLException {
        String sql = "UPDATE reservations SET " +
                "guest_name = ?, guest_email = ?, guest_cpf = ?, " +
                "check_in = ?, check_out = ?, guests = ?, " +
                "rate_plan = ?, company_code = ?, nights = ?, " +
                "subtotal = ?, discount = ?, total = ?, " +
                "status = ?, amount_paid = ?, refund = ? " +
                "WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, res.getGuestName());
            ps.setString(2, res.getGuestEmail());
            ps.setString(3, res.getGuestCpf());
            ps.setObject(4, res.getCheckIn());
            ps.setObject(5, res.getCheckOut());
            ps.setInt(6, res.getGuests());
            ps.setString(7, res.getRatePlan().toString());
            ps.setString(8, res.getCompanyCode());
            ps.setInt(9, res.getNights());
            ps.setDouble(10, res.getSubtotal());
            ps.setDouble(11, res.getDiscount());
            ps.setDouble(12, res.getTotal());
            ps.setString(13, res.getStatus().toString());
            ps.setDouble(14, res.getAmountPaid());
            if (res.getRefund() != null) {
                ps.setDouble(15, res.getRefund());
            } else {
                ps.setNull(15, Types.DOUBLE);
            }
            ps.setLong(16, res.getId());

            ps.executeUpdate();
        }
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservations " +
                "WHERE room_number = ? " +
                "AND status != 'CANCELLED' " +
                "AND check_in < ? " +
                "AND check_out > ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomNumber);
            ps.setObject(2, checkOut);
            ps.setObject(3, checkIn);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) == 0;
            }
        }
    }

    public Long saveMessage(Message msg) throws SQLException {
        String sql = "INSERT INTO messages (channel, \"to\", subject, body) VALUES (?, ?, ?, ?) RETURNING id";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, msg.getChannel());
            ps.setString(2, msg.getTo());
            ps.setString(3, msg.getSubject());
            ps.setString(4, msg.getBody());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        return null;
    }

    public List<Message> getAllMessages() throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, channel, \"to\", subject, body FROM messages ORDER BY id";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Message msg = new Message();
                msg.setId(rs.getLong("id"));
                msg.setChannel(rs.getString("channel"));
                msg.setTo(rs.getString("to"));
                msg.setSubject(rs.getString("subject"));
                msg.setBody(rs.getString("body"));
                messages.add(msg);
            }
        }
        return messages;
    }

    public List<Reservation> getReservationsForOccupancy(LocalDate date) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservations " +
                "WHERE status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT') " +
                "AND check_in <= ? " +
                "AND check_out > ? " +
                "ORDER BY room_number";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, date);
            ps.setObject(2, date);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    public List<Reservation> getReservationsForRevenue(YearMonth month) throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        LocalDate firstDay = month.atDay(1);
        LocalDate lastDay = month.atEndOfMonth();

        String sql = "SELECT * FROM reservations " +
                "WHERE (status = 'CONFIRMED' OR status = 'CHECKED_IN' OR status = 'CHECKED_OUT' OR status = 'CANCELLED') " +
                "AND amount_paid > 0 " +
                "AND check_in >= ? " +
                "AND check_in <= ? " +
                "ORDER BY check_in, id";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, firstDay);
            ps.setObject(2, lastDay);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservations.add(mapReservation(rs));
                }
            }
        }
        return reservations;
    }

    private Reservation mapReservation(ResultSet rs) throws SQLException {
        Reservation res = new Reservation();
        res.setId(rs.getLong("id"));
        res.setRoomNumber(rs.getInt("room_number"));
        res.setGuestName(rs.getString("guest_name"));
        res.setGuestEmail(rs.getString("guest_email"));
        res.setGuestCpf(rs.getString("guest_cpf"));
        res.setCheckIn(rs.getObject("check_in", LocalDate.class));
        res.setCheckOut(rs.getObject("check_out", LocalDate.class));
        res.setGuests(rs.getInt("guests"));
        res.setRatePlan(RatePlanType.fromString(rs.getString("rate_plan")));
        res.setCompanyCode(rs.getString("company_code"));
        res.setNights(rs.getInt("nights"));
        res.setSubtotal(Money.round(rs.getDouble("subtotal")));
        res.setDiscount(Money.round(rs.getDouble("discount")));
        res.setTotal(Money.round(rs.getDouble("total")));
        res.setStatus(ReservationStatus.fromString(rs.getString("status")));
        res.setAmountPaid(Money.round(rs.getDouble("amount_paid")));
        Double refund = rs.getDouble("refund");
        res.setRefund(rs.wasNull() ? null : Money.round(refund));
        return res;
    }
}
