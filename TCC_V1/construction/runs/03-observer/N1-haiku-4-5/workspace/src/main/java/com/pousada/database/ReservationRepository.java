package com.pousada.database;

import com.pousada.model.Reservation;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
  public static Reservation create(
      int roomNumber,
      String guestName,
      String guestEmail,
      String guestCpf,
      LocalDate checkIn,
      LocalDate checkOut,
      int guests,
      String ratePlan,
      int nights,
      BigDecimal subtotal,
      BigDecimal discount,
      BigDecimal total) {
    String sql =
        "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, "
            + "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, "
            + "amount_paid) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0) "
            + "RETURNING id";

    try (Connection conn = Database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, roomNumber);
      stmt.setString(2, guestName);
      stmt.setString(3, guestEmail);
      stmt.setString(4, guestCpf);
      stmt.setDate(5, Date.valueOf(checkIn));
      stmt.setDate(6, Date.valueOf(checkOut));
      stmt.setInt(7, guests);
      stmt.setString(8, ratePlan);
      stmt.setInt(9, nights);
      stmt.setBigDecimal(10, subtotal);
      stmt.setBigDecimal(11, discount);
      stmt.setBigDecimal(12, total);

      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          long id = rs.getLong("id");
          return findById(id).get();
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    throw new RuntimeException("Failed to create reservation");
  }

  public static Optional<Reservation> findById(long id) {
    String sql =
        "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund "
            + "FROM reservations WHERE id = ?";

    try (Connection conn = Database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, id);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapResultSet(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return Optional.empty();
  }

  public static void updateStatus(long id, String newStatus) {
    String sql = "UPDATE reservations SET status = ? WHERE id = ?";
    try (Connection conn = Database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, newStatus);
      stmt.setLong(2, id);
      stmt.execute();
      conn.commit();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static void confirmReservation(long id, BigDecimal amountPaid) {
    String sql =
        "UPDATE reservations SET status = ?, amount_paid = ? WHERE id = ?";
    try (Connection conn = Database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, "CONFIRMED");
      stmt.setBigDecimal(2, amountPaid);
      stmt.setLong(3, id);
      stmt.execute();
      conn.commit();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static void cancelReservation(long id, BigDecimal refund) {
    String sql =
        "UPDATE reservations SET status = ?, refund = ? WHERE id = ?";
    try (Connection conn = Database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, "CANCELLED");
      stmt.setBigDecimal(2, refund);
      stmt.setLong(3, id);
      stmt.execute();
      conn.commit();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<Reservation> findByRoomAndDateRange(int roomNumber, LocalDate start, LocalDate end) {
    List<Reservation> reservations = new ArrayList<>();
    String sql =
        "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund "
            + "FROM reservations "
            + "WHERE room_number = ? AND NOT status = ? "
            + "AND check_in < ? AND check_out > ?";

    try (Connection conn = Database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, roomNumber);
      stmt.setString(2, "CANCELLED");
      stmt.setDate(3, Date.valueOf(end));
      stmt.setDate(4, Date.valueOf(start));
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          reservations.add(mapResultSet(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return reservations;
  }

  public static List<Reservation> findConfirmedByDate(LocalDate date) {
    List<Reservation> reservations = new ArrayList<>();
    String sql =
        "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund "
            + "FROM reservations "
            + "WHERE (status = ? OR status = ? OR status = ?) "
            + "AND check_in <= ? AND check_out > ? "
            + "ORDER BY room_number";

    try (Connection conn = Database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setString(1, "CONFIRMED");
      stmt.setString(2, "CHECKED_IN");
      stmt.setString(3, "CHECKED_OUT");
      stmt.setDate(4, Date.valueOf(date));
      stmt.setDate(5, Date.valueOf(date));
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          reservations.add(mapResultSet(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return reservations;
  }

  public static List<Reservation> findByMonthAndNonCancelled(int year, int month) {
    List<Reservation> reservations = new ArrayList<>();
    String sql =
        "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, "
            + "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund "
            + "FROM reservations "
            + "WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ? "
            + "AND status != ? "
            + "ORDER BY check_in, id";

    try (Connection conn = Database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, year);
      stmt.setInt(2, month);
      stmt.setString(3, "CANCELLED");
      try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
          reservations.add(mapResultSet(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
    return reservations;
  }

  private static Reservation mapResultSet(ResultSet rs) throws SQLException {
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
        rs.getString("status"),
        rs.getBigDecimal("amount_paid"),
        rs.getBigDecimal("refund"));
  }
}
