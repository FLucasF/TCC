package com.pousada.marealta.repository;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.ReservationStatus;
import com.pousada.marealta.domain.RatePlan;
import com.pousada.marealta.persistence.Database;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
  public static Reservation create(Reservation reservation) throws SQLException {
    String sql = "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, " +
        "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

    try (Connection conn = Database.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, reservation.getRoomNumber());
      pstmt.setString(2, reservation.getGuestName());
      pstmt.setString(3, reservation.getGuestEmail());
      pstmt.setString(4, reservation.getGuestCpf());
      pstmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
      pstmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
      pstmt.setInt(7, reservation.getGuests());
      pstmt.setString(8, reservation.getRatePlan().name());
      pstmt.setInt(9, reservation.getNights());
      pstmt.setBigDecimal(10, reservation.getSubtotal());
      pstmt.setBigDecimal(11, reservation.getDiscount());
      pstmt.setBigDecimal(12, reservation.getTotal());
      pstmt.setString(13, reservation.getStatus().name());
      pstmt.setBigDecimal(14, reservation.getAmountPaid());

      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return findById(rs.getLong("id"));
        }
      }
    }
    throw new SQLException("Failed to create reservation");
  }

  public static Reservation findById(long id) throws SQLException {
    String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
        "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund " +
        "FROM reservations WHERE id = ?";

    try (Connection conn = Database.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setLong(1, id);
      try (ResultSet rs = pstmt.executeQuery()) {
        if (rs.next()) {
          return mapToReservation(rs);
        }
      }
    }
    return null;
  }

  public static void update(Reservation reservation) throws SQLException {
    String sql = "UPDATE reservations SET status = ?, amount_paid = ?, refund = ? WHERE id = ?";

    try (Connection conn = Database.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setString(1, reservation.getStatus().name());
      pstmt.setBigDecimal(2, reservation.getAmountPaid());
      pstmt.setBigDecimal(3, reservation.getRefund());
      pstmt.setLong(4, reservation.getId());
      pstmt.executeUpdate();
    }
  }

  public static List<Reservation> findByRoomAndDate(int roomNumber, LocalDate date) throws SQLException {
    String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
        "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund " +
        "FROM reservations WHERE room_number = ? AND check_in <= ? AND check_out > ? " +
        "AND status != ?";

    List<Reservation> reservations = new ArrayList<>();
    try (Connection conn = Database.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setInt(1, roomNumber);
      pstmt.setDate(2, Date.valueOf(date));
      pstmt.setDate(3, Date.valueOf(date));
      pstmt.setString(4, ReservationStatus.CANCELLED.name());

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          reservations.add(mapToReservation(rs));
        }
      }
    }
    return reservations;
  }

  public static List<Reservation> findByMonthStarting(int year, int month) throws SQLException {
    LocalDate monthStart = LocalDate.of(year, month, 1);
    LocalDate monthEnd = monthStart.plusMonths(1);

    String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, " +
        "guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund " +
        "FROM reservations WHERE check_in >= ? AND check_in < ? " +
        "AND status IN (?, ?, ?) " +
        "ORDER BY check_in ASC, id ASC";

    List<Reservation> reservations = new ArrayList<>();
    try (Connection conn = Database.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
      pstmt.setDate(1, Date.valueOf(monthStart));
      pstmt.setDate(2, Date.valueOf(monthEnd));
      pstmt.setString(3, ReservationStatus.CONFIRMED.name());
      pstmt.setString(4, ReservationStatus.CHECKED_IN.name());
      pstmt.setString(5, ReservationStatus.CHECKED_OUT.name());

      try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
          reservations.add(mapToReservation(rs));
        }
      }
    }
    return reservations;
  }

  private static Reservation mapToReservation(ResultSet rs) throws SQLException {
    Reservation reservation = new Reservation(
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
        rs.getBigDecimal("total")
    );
    reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));
    reservation.setAmountPaid(rs.getBigDecimal("amount_paid"));
    reservation.setRefund(rs.getBigDecimal("refund"));
    return reservation;
  }
}
