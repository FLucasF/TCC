package com.pousadamarealta.service;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import com.pousadamarealta.dao.ReservationDAO;
import com.pousadamarealta.dao.RoomDAO;
import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.models.Room;

public class ReportService {
    public static String generateOccupancyReport(LocalDate date) throws SQLException {
        List<Room> allRooms = RoomDAO.getAllRooms();
        List<Reservation> reservations = ReservationDAO.getByRoomAndDate(date);

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation res : reservations) {
            csv.append(res.roomNumber()).append(",");
            csv.append(RoomDAO.getRoomByNumber(res.roomNumber()).type()).append(",");
            csv.append(res.id()).append(",");
            csv.append(res.checkIn()).append(",");
            csv.append(res.checkOut()).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());
        return csv.toString();
    }

    public static String generateRevenueReport(YearMonth month) throws SQLException {
        List<Reservation> reservations = ReservationDAO.getByMonthAndStatus(
            month.getYear(), month.getMonthValue());

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (Reservation res : reservations) {
            csv.append(res.id()).append(",");
            csv.append(res.roomNumber()).append(",");
            csv.append(res.checkIn()).append(",");
            csv.append(res.checkOut()).append(",");
            csv.append(res.status()).append(",");

            java.math.BigDecimal amount = res.amountPaid();
            if (res.refund() != null && res.refund().compareTo(java.math.BigDecimal.ZERO) > 0) {
                amount = amount.subtract(res.refund());
            }

            csv.append(String.format(java.util.Locale.US, "%.2f", amount)).append("\n");
            total = total.add(amount);
        }

        csv.append("total,,,,").append(String.format(java.util.Locale.US, "%.2f", total));
        return csv.toString();
    }
}
