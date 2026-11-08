package com.pousada.services;

import com.pousada.database.Database;
import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.utils.PricingUtils;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReservationService {
    private final Database db;

    public ReservationService(Database db) {
        this.db = db;
    }

    public Reservation createReservation(int roomNumber, String guestName, String guestEmail, String guestCpf,
                                         LocalDate checkIn, LocalDate checkOut, int guests, String ratePlan,
                                         Room room) {
        int nights = PricingUtils.calculateNights(checkIn, checkOut);
        BigDecimal subtotal = PricingUtils.calculateSubtotal(room.nightlyRate, checkIn, checkOut);
        BigDecimal discount = PricingUtils.calculateDiscount(ratePlan, subtotal);
        BigDecimal total = PricingUtils.calculateTotal(subtotal, discount);

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO reservations (room_number, guest_name, guest_email, guest_cpf, " +
                             "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                             "RETURNING id",
                     Statement.RETURN_GENERATED_KEYS)) {

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
            stmt.setString(13, "PENDING");
            stmt.setBigDecimal(14, BigDecimal.ZERO);
            stmt.setNull(15, java.sql.Types.NUMERIC);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    return getReservationById(id);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar reserva", e);
        }
        return null;
    }

    public Reservation getReservationById(long id) {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "SELECT id, room_number, guest_name, guest_email, guest_cpf, " +
                    "check_in, check_out, guests, rate_plan, nights, subtotal, discount, total, status, amount_paid, refund " +
                    "FROM reservations WHERE id = " + id;
            ResultSet rs = stmt.executeQuery(sql);

            if (rs.next()) {
                return mapReservation(rs);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar reserva", e);
        }
        return null;
    }

    public Reservation confirmReservation(long id, BigDecimal amountPaid) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE reservations SET status = ?, amount_paid = ? WHERE id = ?")) {

            stmt.setString(1, "CONFIRMED");
            stmt.setBigDecimal(2, amountPaid);
            stmt.setLong(3, id);
            stmt.executeUpdate();

            return getReservationById(id);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao confirmar reserva", e);
        }
    }

    public Reservation checkIn(long id) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE reservations SET status = ? WHERE id = ?")) {

            stmt.setString(1, "CHECKED_IN");
            stmt.setLong(2, id);
            stmt.executeUpdate();

            return getReservationById(id);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer check-in", e);
        }
    }

    public Reservation checkOut(long id) {
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE reservations SET status = ? WHERE id = ?")) {

            stmt.setString(1, "CHECKED_OUT");
            stmt.setLong(2, id);
            stmt.executeUpdate();

            return getReservationById(id);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer check-out", e);
        }
    }

    public Reservation cancelReservation(long id, String ratePlan, BigDecimal amountPaid) {
        BigDecimal refund = BigDecimal.ZERO;

        if ("STANDARD".equals(ratePlan)) {
            refund = amountPaid;
        }

        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE reservations SET status = ?, refund = ? WHERE id = ?")) {

            stmt.setString(1, "CANCELLED");
            stmt.setBigDecimal(2, refund);
            stmt.setLong(3, id);
            stmt.executeUpdate();

            return getReservationById(id);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao cancelar reserva", e);
        }
    }

    public boolean isRoomAvailable(int roomNumber, LocalDate checkIn, LocalDate checkOut) {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = "SELECT COUNT(*) as count FROM reservations " +
                    "WHERE room_number = " + roomNumber +
                    " AND status != 'CANCELLED'" +
                    " AND check_in < '" + checkOut + "' AND check_out > '" + checkIn + "'";

            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return rs.getInt("count") == 0;
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar disponibilidade", e);
        }
        return true;
    }

    private Reservation mapReservation(ResultSet rs) throws java.sql.SQLException {
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
                rs.getBigDecimal("refund")
        );
    }
}
