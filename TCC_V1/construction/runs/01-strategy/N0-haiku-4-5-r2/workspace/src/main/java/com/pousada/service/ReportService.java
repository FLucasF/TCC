package com.pousada.service;

import com.pousada.database.Database;
import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.util.MoneyRounder;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ReportService {
    public static String getOccupancyReport(LocalDate date) throws Exception {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> reservations = getReservationsForNight(date);
        int occupied = 0;

        for (Reservation res : reservations) {
            csv.append(String.format("%d,%s,%d,%s,%s\n",
                res.getRoomNumber(),
                RoomService.getRoomByNumber(res.getRoomNumber()).getType(),
                res.getId(),
                res.getCheckIn().toString(),
                res.getCheckOut().toString()
            ));
            occupied++;
        }

        csv.append(String.format("total,%d/5\n", occupied));

        return csv.toString();
    }

    public static String getRevenueReport(YearMonth month) throws Exception {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> reservations = getReservationsStartingInMonth(month);
        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amount = res.getAmountPaid();
            if (res.getRefund() != null && res.getRefund().compareTo(BigDecimal.ZERO) > 0) {
                amount = MoneyRounder.round(amount.subtract(res.getRefund()));
            }

            csv.append(String.format("%d,%d,%s,%s,%s,%.2f\n",
                res.getId(),
                res.getRoomNumber(),
                res.getCheckIn().toString(),
                res.getCheckOut().toString(),
                res.getStatus(),
                amount.doubleValue()
            ));

            total = total.add(amount);
        }

        total = MoneyRounder.round(total);
        csv.append(String.format("total,%.2f\n", total.doubleValue()));

        return csv.toString();
    }

    private static List<Reservation> getReservationsForNight(LocalDate date) throws Exception {
        List<Reservation> reservations = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund FROM reservations WHERE check_in <= ? AND check_out > ? AND status IN ('CHECKED_IN', 'CHECKED_OUT', 'CONFIRMED') ORDER BY room_number")) {

            stmt.setDate(1, Date.valueOf(date));
            stmt.setDate(2, Date.valueOf(date));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation res = mapReservation(rs);
                    reservations.add(res);
                }
            }
        }

        return reservations;
    }

    private static List<Reservation> getReservationsStartingInMonth(YearMonth month) throws Exception {
        List<Reservation> reservations = new ArrayList<>();

        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth().plusDays(1);

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                "SELECT id, room_number, guest_name, guest_email, guest_cpf, check_in, check_out, guests, rate_plan, company_code, nights, subtotal, discount, total, status, amount_paid, refund FROM reservations WHERE check_in >= ? AND check_in < ? AND (amount_paid > 0 OR refund > 0) ORDER BY check_in, id")) {

            stmt.setDate(1, Date.valueOf(start));
            stmt.setDate(2, Date.valueOf(end));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Reservation res = mapReservation(rs);
                    reservations.add(res);
                }
            }
        }

        return reservations;
    }

    private static Reservation mapReservation(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getLong("id"),
            rs.getInt("room_number"),
            rs.getString("guest_name"),
            rs.getString("guest_email"),
            rs.getString("guest_cpf"),
            rs.getDate("check_in").toLocalDate(),
            rs.getDate("check_out").toLocalDate(),
            rs.getInt("guests"),
            com.pousada.domain.RatePlan.valueOf(rs.getString("rate_plan")),
            rs.getString("company_code"),
            rs.getInt("nights"),
            rs.getBigDecimal("subtotal"),
            rs.getBigDecimal("discount"),
            rs.getBigDecimal("total"),
            ReservationStatus.valueOf(rs.getString("status")),
            rs.getBigDecimal("amount_paid"),
            rs.getBigDecimal("refund")
        );
    }

}
