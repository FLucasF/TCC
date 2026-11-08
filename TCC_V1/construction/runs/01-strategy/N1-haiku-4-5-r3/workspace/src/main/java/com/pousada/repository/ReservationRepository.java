package com.pousada.repository;

import com.pousada.infrastructure.Database;
import com.pousada.model.Reservation;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepository {
  public Reservation save(Reservation reservation) {
    String sql = """
        INSERT INTO reservations
        (room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan,
         company_code, nights, subtotal, discount, total, status, amount_paid, refund)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    try (Connection conn = Database.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      stmt.setInt(1, reservation.getRoomNumber());
      stmt.setString(2, reservation.getGuestName());
      stmt.setString(3, reservation.getGuestEmail());
      stmt.setString(4, reservation.getGuestCpf());
      stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
      stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
      stmt.setInt(7, reservation.getGuests());
      stmt.setString(8, reservation.getRatePlan().toString());
      stmt.setString(9, reservation.getCompanyCode());
      stmt.setInt(10, reservation.getNights());
      stmt.setBigDecimal(11, reservation.getSubtotal());
      stmt.setBigDecimal(12, reservation.getDiscount());
      stmt.setBigDecimal(13, reservation.getTotal());
      stmt.setString(14, reservation.getStatus().toString());
      stmt.setBigDecimal(15, reservation.getAmountPaid());
      stmt.setObject(16, reservation.getRefund());

      stmt.executeUpdate();

      try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          reservation.setId(generatedKeys.getLong(1));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save reservation", e);
    }

    return reservation;
  }

  public Optional<Reservation> findById(long id) {
    String sql = """
        SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
               rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund
        FROM reservations WHERE id = ?
        """;

    try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setLong(1, id);
      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
        return Optional.of(mapReservation(rs));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find reservation", e);
    }

    return Optional.empty();
  }

  public void update(Reservation reservation) {
    String sql = """
        UPDATE reservations SET room_number = ?, guest_name = ?, guest_email = ?, guest_cpf = ?,
               check_in = ?, check_out = ?, guests = ?, rate_plan = ?, company_code = ?,
               nights = ?, subtotal = ?, discount = ?, total = ?, status = ?, amount_paid = ?, refund = ?
        WHERE id = ?
        """;

    try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, reservation.getRoomNumber());
      stmt.setString(2, reservation.getGuestName());
      stmt.setString(3, reservation.getGuestEmail());
      stmt.setString(4, reservation.getGuestCpf());
      stmt.setDate(5, Date.valueOf(reservation.getCheckIn()));
      stmt.setDate(6, Date.valueOf(reservation.getCheckOut()));
      stmt.setInt(7, reservation.getGuests());
      stmt.setString(8, reservation.getRatePlan().toString());
      stmt.setString(9, reservation.getCompanyCode());
      stmt.setInt(10, reservation.getNights());
      stmt.setBigDecimal(11, reservation.getSubtotal());
      stmt.setBigDecimal(12, reservation.getDiscount());
      stmt.setBigDecimal(13, reservation.getTotal());
      stmt.setString(14, reservation.getStatus().toString());
      stmt.setBigDecimal(15, reservation.getAmountPaid());
      stmt.setObject(16, reservation.getRefund());
      stmt.setLong(17, reservation.getId());

      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to update reservation", e);
    }
  }

  public List<Reservation> findByRoomAndDates(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
    String sql = """
        SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
               rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund
        FROM reservations
        WHERE room_number = ? AND status != 'CANCELLED'
        AND check_in < ? AND check_out > ?
        """;

    List<Reservation> reservations = new ArrayList<>();
    try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, roomNumber);
      stmt.setDate(2, Date.valueOf(checkOut));
      stmt.setDate(3, Date.valueOf(checkIn));

      ResultSet rs = stmt.executeQuery();
      while (rs.next()) {
        reservations.add(mapReservation(rs));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find reservations by room and dates", e);
    }

    return reservations;
  }

  public List<Reservation> findConfirmedByRoomAndDate(int roomNumber, LocalDate date) {
    String sql = """
        SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
               rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund
        FROM reservations
        WHERE room_number = ? AND status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
        AND check_in <= ? AND check_out > ?
        ORDER BY check_in, id
        """;

    List<Reservation> reservations = new ArrayList<>();
    try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, roomNumber);
      stmt.setDate(2, Date.valueOf(date));
      stmt.setDate(3, Date.valueOf(date));

      ResultSet rs = stmt.executeQuery();
      while (rs.next()) {
        reservations.add(mapReservation(rs));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find confirmed reservations", e);
    }

    return reservations;
  }

  public List<Reservation> findByMonthWithRevenue(int year, int month) {
    String sql = """
        SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests,
               rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund
        FROM reservations
        WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ?
        AND amount_paid > 0
        ORDER BY check_in, id
        """;

    List<Reservation> reservations = new ArrayList<>();
    try (Connection conn = Database.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
      stmt.setInt(1, year);
      stmt.setInt(2, month);

      ResultSet rs = stmt.executeQuery();
      while (rs.next()) {
        reservations.add(mapReservation(rs));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find reservations by month", e);
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
    r.setRatePlan(Reservation.RatePlan.valueOf(rs.getString("rate_plan")));
    r.setCompanyCode(rs.getString("company_code"));
    r.setNights(rs.getInt("nights"));
    r.setSubtotal(rs.getBigDecimal("subtotal"));
    r.setDiscount(rs.getBigDecimal("discount"));
    r.setTotal(rs.getBigDecimal("total"));
    r.setStatus(Reservation.Status.valueOf(rs.getString("status")));
    r.setAmountPaid(rs.getBigDecimal("amount_paid"));
    r.setRefund(rs.getBigDecimal("refund"));
    return r;
  }
}
