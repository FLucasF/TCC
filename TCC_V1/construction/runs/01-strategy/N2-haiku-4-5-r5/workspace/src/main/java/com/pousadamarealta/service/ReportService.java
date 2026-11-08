package com.pousadamarealta.service;

import com.pousadamarealta.api.ApiException;
import com.pousadamarealta.database.ReservationDao;
import com.pousadamarealta.database.RoomDao;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String getOccupancyReport(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMAT);
            List<Room> rooms = RoomDao.getAllRooms();

            StringBuilder csv = new StringBuilder();
            csv.append("room,type,reservation,check_in,check_out\n");

            int occupied = 0;
            for (Room room : rooms) {
                List<Reservation> reservations = ReservationDao.getConfirmedReservationsByRoomAndNight(room.number(), date);

                if (!reservations.isEmpty()) {
                    occupied++;
                    for (Reservation res : reservations) {
                        csv.append(formatCsvLine(
                            String.valueOf(room.number()),
                            room.type().toString(),
                            String.valueOf(res.getId()),
                            formatCsvDate(res.getCheckIn()),
                            formatCsvDate(res.getCheckOut())
                        )).append("\n");
                    }
                }
            }

            csv.append("total,").append(occupied).append("/").append(rooms.size()).append("\n");

            return csv.toString();
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        } catch (Exception e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid date format");
        }
    }

    public static String getRevenueReport(String monthStr) {
        try {
            String[] parts = monthStr.split("-");
            if (parts.length != 2) {
                throw new ApiException(400, "INVALID_REQUEST", "Invalid month format");
            }

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            List<Reservation> reservations = ReservationDao.getReservationsStartingInMonth(year, month);

            StringBuilder csv = new StringBuilder();
            csv.append("reservation,room,check_in,check_out,status,amount\n");

            BigDecimal total = BigDecimal.ZERO;
            for (Reservation res : reservations) {
                BigDecimal amount = res.getAmountPaid();
                if (res.getRefund() != null) {
                    amount = amount.subtract(res.getRefund());
                }

                csv.append(formatCsvLine(
                    String.valueOf(res.getId()),
                    String.valueOf(res.getRoomNumber()),
                    formatCsvDate(res.getCheckIn()),
                    formatCsvDate(res.getCheckOut()),
                    res.getStatus().toString(),
                    formatCsvMoney(amount)
                )).append("\n");

                total = total.add(amount);
            }

            csv.append("total,,,,").append(formatCsvMoney(total)).append("\n");

            return csv.toString();
        } catch (SQLException e) {
            throw new RuntimeException("Database error", e);
        } catch (NumberFormatException e) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid month format");
        }
    }

    private static String formatCsvDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private static String formatCsvMoney(BigDecimal value) {
        return String.format("%.2f", value);
    }

    private static String formatCsvLine(String... fields) {
        return String.join(",", fields);
    }
}
