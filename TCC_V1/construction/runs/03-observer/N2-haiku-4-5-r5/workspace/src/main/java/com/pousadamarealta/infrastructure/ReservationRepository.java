package com.pousadamarealta.infrastructure;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
  public static Reservation insert(
      int roomNumber,
      String guestName,
      String guestEmail,
      String guestCpf,
      LocalDate checkIn,
      LocalDate checkOut,
      int guests,
      RatePlan ratePlan,
      int nights,
      BigDecimal subtotal,
      BigDecimal discount,
      BigDecimal total)
      throws SQLException {
    try (Connection conn = Database.getConnection()) {
      var stmt =
          conn.prepareStatement(
              "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status) "
                  + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
              Statement.RETURN_GENERATED_KEYS);
      stmt.setInt(1, roomNumber);
      stmt.setString(2, guestName);
      stmt.setString(3, guestEmail);
      stmt.setString(4, guestCpf);
      stmt.setDate(5, Date.valueOf(checkIn));
      stmt.setDate(6, Date.valueOf(checkOut));
      stmt.setInt(7, guests);
      stmt.setString(8, ratePlan.value);
      stmt.setInt(9, nights);
      stmt.setBigDecimal(10, subtotal);
      stmt.setBigDecimal(11, discount);
      stmt.setBigDecimal(12, total);
      stmt.setString(13, ReservationStatus.PENDING.value);

      var rs = stmt.executeQuery();
      rs.next();
      long id = rs.getLong(1);

      return new Reservation(
          id,
          roomNumber,
          guestName,
          guestEmail,
          guestCpf,
          checkIn,
          checkOut,
          guests,
          ratePlan,
          nights,
          subtotal,
          discount,
          total,
          ReservationStatus.PENDING,
          BigDecimal.ZERO,
          null);
    }
  }

  public static Optional<Reservation> findById(long id) throws SQLException {
    try (Connection conn = Database.getConnection()) {
      var stmt = conn.prepareStatement("SELECT * FROM reservations WHERE id = ?");
      stmt.setLong(1, id);
      var rs = stmt.executeQuery();
      if (rs.next()) {
        return Optional.of(mapRow(rs));
      }
      return Optional.empty();
    }
  }

  public static void updateStatus(long id, ReservationStatus status) throws SQLException {
    try (Connection conn = Database.getConnection()) {
      var stmt = conn.prepareStatement("UPDATE reservations SET status = ? WHERE id = ?");
      stmt.setString(1, status.value);
      stmt.setLong(2, id);
      stmt.executeUpdate();
    }
  }

  public static void confirmPayment(long id, BigDecimal amountPaid) throws SQLException {
    try (Connection conn = Database.getConnection()) {
      var stmt =
          conn.prepareStatement(
              "UPDATE reservations SET status = ?, amount_paid = ? WHERE id = ?");
      stmt.setString(1, ReservationStatus.CONFIRMED.value);
      stmt.setBigDecimal(2, amountPaid);
      stmt.setLong(3, id);
      stmt.executeUpdate();
    }
  }

  public static void cancelWithRefund(long id, BigDecimal refundAmount) throws SQLException {
    try (Connection conn = Database.getConnection()) {
      var stmt =
          conn.prepareStatement("UPDATE reservations SET status = ?, refund = ? WHERE id = ?");
      stmt.setString(1, ReservationStatus.CANCELLED.value);
      stmt.setBigDecimal(2, refundAmount);
      stmt.setLong(3, id);
      stmt.executeUpdate();
    }
  }

  public static List<Reservation> findByRoomAndDateRange(
      int roomNumber, LocalDate start, LocalDate end) throws SQLException {
    List<Reservation> reservations = new ArrayList<>();
    try (Connection conn = Database.getConnection()) {
      var stmt =
          conn.prepareStatement(
              "SELECT * FROM reservations WHERE room_number = ? AND check_in <= ? AND check_out > ? AND status != ? ORDER BY check_in");
      stmt.setInt(1, roomNumber);
      stmt.setDate(2, Date.valueOf(end.minusDays(1)));
      stmt.setDate(3, Date.valueOf(start));
      stmt.setString(4, ReservationStatus.CANCELLED.value);
      var rs = stmt.executeQuery();
      while (rs.next()) {
        reservations.add(mapRow(rs));
      }
    }
    return reservations;
  }

  public static List<Reservation> findByMonthStartingInMonth(int month, int year)
      throws SQLException {
    List<Reservation> reservations = new ArrayList<>();
    LocalDate startOfMonth = LocalDate.of(year, month, 1);
    LocalDate endOfMonth = startOfMonth.plusMonths(1).minusDays(1);

    try (Connection conn = Database.getConnection()) {
      var stmt =
          conn.prepareStatement(
              "SELECT * FROM reservations WHERE check_in >= ? AND check_in <= ? AND status != ? ORDER BY check_in, id");
      stmt.setDate(1, Date.valueOf(startOfMonth));
      stmt.setDate(2, Date.valueOf(endOfMonth));
      stmt.setString(3, ReservationStatus.CANCELLED.value);
      var rs = stmt.executeQuery();
      while (rs.next()) {
        reservations.add(mapRow(rs));
      }
    }
    return reservations;
  }

  public static List<Reservation> findConfirmedOnDate(LocalDate date) throws SQLException {
    List<Reservation> reservations = new ArrayList<>();
    try (Connection conn = Database.getConnection()) {
      var stmt =
          conn.prepareStatement(
              "SELECT * FROM reservations WHERE check_in <= ? AND check_out > ? AND status IN (?, ?, ?) ORDER BY room_number");
      stmt.setDate(1, Date.valueOf(date));
      stmt.setDate(2, Date.valueOf(date));
      stmt.setString(3, ReservationStatus.CONFIRMED.value);
      stmt.setString(4, ReservationStatus.CHECKED_IN.value);
      stmt.setString(5, ReservationStatus.CHECKED_OUT.value);
      var rs = stmt.executeQuery();
      while (rs.next()) {
        reservations.add(mapRow(rs));
      }
    }
    return reservations;
  }

  private static Reservation mapRow(java.sql.ResultSet rs) throws SQLException {
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
        RatePlan.from(rs.getString("rate_plan")),
        rs.getInt("nights"),
        rs.getBigDecimal("subtotal"),
        rs.getBigDecimal("discount"),
        rs.getBigDecimal("total"),
        ReservationStatus.from(rs.getString("status")),
        rs.getBigDecimal("amount_paid"),
        refund);
  }
}
