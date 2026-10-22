package com.pousada.service;

import com.pousada.db.Database;
import com.pousada.domain.Reservation;
import com.pousada.repository.RoomRepository;
import com.pousada.utils.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class ReportService {
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private final RoomRepository roomRepo;

    public ReportService(RoomRepository roomRepo) {
        this.roomRepo = roomRepo;
    }

    public String generateOccupancyReport(LocalDate date) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;

        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                """
                SELECT r.id, r.room_number, r.check_in, r.check_out
                FROM reservations r
                WHERE r.status IN ('CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT')
                AND r.check_in <= ? AND r.check_out > ?
                ORDER BY r.room_number
                """
            );
            stmt.setDate(1, java.sql.Date.valueOf(date));
            stmt.setDate(2, java.sql.Date.valueOf(date));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                long reservationId = rs.getLong("id");
                int roomNumber = rs.getInt("room_number");
                LocalDate checkIn = rs.getDate("check_in").toLocalDate();
                LocalDate checkOut = rs.getDate("check_out").toLocalDate();

                var room = roomRepo.findById(roomNumber);
                csv.append(roomNumber).append(",")
                    .append(room.getType()).append(",")
                    .append(reservationId).append(",")
                    .append(checkIn).append(",")
                    .append(checkOut).append("\n");

                occupiedCount++;
            }
        }

        int totalRooms = 5;
        csv.append("total,").append(occupiedCount).append("/").append(totalRooms).append("\n");

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;

        try (Connection conn = Database.getConnection()) {
            var stmt = conn.prepareStatement(
                """
                SELECT id, room_number, check_in, check_out, status, amount_paid, refund
                FROM reservations
                WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ?
                AND (amount_paid > 0 OR refund > 0)
                ORDER BY check_in, id
                """
            );
            stmt.setInt(1, month.getYear());
            stmt.setInt(2, month.getMonthValue());

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                long reservationId = rs.getLong("id");
                int roomNumber = rs.getInt("room_number");
                LocalDate checkIn = rs.getDate("check_in").toLocalDate();
                LocalDate checkOut = rs.getDate("check_out").toLocalDate();
                String status = rs.getString("status");
                BigDecimal amountPaid = rs.getBigDecimal("amount_paid");
                BigDecimal refund = rs.getBigDecimal("refund");

                BigDecimal netAmount = amountPaid;
                if (refund != null && refund.compareTo(BigDecimal.ZERO) > 0) {
                    netAmount = Money.subtract(amountPaid, refund);
                }

                csv.append(reservationId).append(",")
                    .append(roomNumber).append(",")
                    .append(checkIn).append(",")
                    .append(checkOut).append(",")
                    .append(status).append(",")
                    .append(String.format("%.2f", netAmount)).append("\n");

                totalAmount = Money.add(totalAmount, netAmount);
            }
        }

        csv.append("total,")
            .append(String.format("%.2f", totalAmount)).append("\n");

        return csv.toString();
    }
}
