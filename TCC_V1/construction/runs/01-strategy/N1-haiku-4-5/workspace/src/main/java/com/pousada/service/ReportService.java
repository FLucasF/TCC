package com.pousada.service;

import com.pousada.database.ReservationRepository;
import com.pousada.database.RoomRepository;
import com.pousada.model.Reservation;
import com.pousada.model.ReservationStatus;
import com.pousada.model.Room;
import com.pousada.util.Formatter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ReportService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    public ReportService(RoomRepository roomRepository, ReservationRepository reservationRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
    }

    public String generateOccupancyReport(LocalDate date, Connection conn) throws SQLException {
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;

        try (var stmt = conn.prepareStatement(
            "SELECT id, room_number, check_in, check_out FROM reservations " +
            "WHERE status != ? AND check_in <= ? AND check_out > ? " +
            "ORDER BY room_number")) {

            stmt.setString(1, ReservationStatus.CANCELLED.toString());
            stmt.setDate(2, Date.valueOf(date));
            stmt.setDate(3, Date.valueOf(date));

            ResultSet rs = stmt.executeQuery();
            var rows = new ArrayList<OccupancyRow>();
            while (rs.next()) {
                rows.add(new OccupancyRow(
                    rs.getLong("id"),
                    rs.getInt("room_number"),
                    rs.getDate("check_in").toLocalDate(),
                    rs.getDate("check_out").toLocalDate()
                ));
            }

            for (var row : rows) {
                Room room = allRooms.stream()
                    .filter(r -> r.getNumber() == row.roomNumber)
                    .findFirst()
                    .orElse(null);

                if (room != null) {
                    csv.append(row.roomNumber).append(",")
                       .append(room.getType().getDisplayName()).append(",")
                       .append(row.reservationId).append(",")
                       .append(Formatter.formatDateIso(row.checkIn)).append(",")
                       .append(Formatter.formatDateIso(row.checkOut)).append("\n");
                    occupiedCount++;
                }
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(allRooms.size());

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth yearMonth, Connection conn) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        try (var stmt = conn.prepareStatement(
            "SELECT id, room_number, check_in, check_out, status, amount_paid, refund " +
            "FROM reservations " +
            "WHERE EXTRACT(YEAR FROM check_in) = ? AND EXTRACT(MONTH FROM check_in) = ? " +
            "AND (status IN (?, ?, ?) OR (status = ? AND amount_paid > 0)) " +
            "ORDER BY check_in, id")) {

            stmt.setInt(1, yearMonth.getYear());
            stmt.setInt(2, yearMonth.getMonthValue());
            stmt.setString(3, ReservationStatus.CONFIRMED.toString());
            stmt.setString(4, ReservationStatus.CHECKED_IN.toString());
            stmt.setString(5, ReservationStatus.CHECKED_OUT.toString());
            stmt.setString(6, ReservationStatus.CANCELLED.toString());

            ResultSet rs = stmt.executeQuery();
            var rows = new ArrayList<RevenueRow>();
            while (rs.next()) {
                rows.add(new RevenueRow(
                    rs.getLong("id"),
                    rs.getInt("room_number"),
                    rs.getDate("check_in").toLocalDate(),
                    rs.getDate("check_out").toLocalDate(),
                    rs.getString("status"),
                    rs.getBigDecimal("amount_paid"),
                    rs.getBigDecimal("refund")
                ));
            }

            for (var row : rows) {
                BigDecimal amount = row.amountPaid;
                if (row.refund != null && row.refund.compareTo(BigDecimal.ZERO) > 0) {
                    amount = row.amountPaid.subtract(row.refund);
                }

                csv.append(row.reservationId).append(",")
                   .append(row.roomNumber).append(",")
                   .append(Formatter.formatDateIso(row.checkIn)).append(",")
                   .append(Formatter.formatDateIso(row.checkOut)).append(",")
                   .append(row.status).append(",")
                   .append(Formatter.formatCurrencyForCsv(amount)).append("\n");

                total = total.add(amount);
            }
        }

        csv.append("total,,,,").append(Formatter.formatCurrencyForCsv(total));

        return csv.toString();
    }

    private record OccupancyRow(long reservationId, int roomNumber, LocalDate checkIn, LocalDate checkOut) {}
    private record RevenueRow(long reservationId, int roomNumber, LocalDate checkIn, LocalDate checkOut,
                             String status, BigDecimal amountPaid, BigDecimal refund) {}
}
