package com.pousadamarealta.services;

import com.pousadamarealta.database.ReservationRepository;
import com.pousadamarealta.database.RoomRepository;
import com.pousadamarealta.models.Reservation;
import com.pousadamarealta.models.Room;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String generateOccupancyReport(LocalDate date) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> reservations = ReservationRepository.findByDateAndPaid(date);
        for (Reservation res : reservations) {
            Room room = RoomRepository.findById(res.roomNumber);
            if (room != null) {
                csv.append(res.roomNumber).append(",");
                csv.append(room.type).append(",");
                csv.append(res.id).append(",");
                csv.append(res.checkIn.format(DATE_FORMAT)).append(",");
                csv.append(res.checkOut.format(DATE_FORMAT)).append("\n");
            }
        }

        int occupiedRooms = reservations.stream()
            .map(r -> r.roomNumber)
            .distinct()
            .toArray()
            .length;
        int totalRooms = (int) RoomRepository.findAll().stream().count();

        csv.append("total,").append(occupiedRooms).append("/").append(totalRooms).append("\n");

        return csv.toString();
    }

    public static String generateRevenueReport(YearMonth month) throws SQLException {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> reservations = ReservationRepository.findByMonthAndPaid(month.getYear(), month.getMonthValue());
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amount = res.amountPaid;
            if (res.refund != null && res.refund.compareTo(BigDecimal.ZERO) > 0) {
                amount = amount.subtract(res.refund);
            }

            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(res.id).append(",");
                csv.append(res.roomNumber).append(",");
                csv.append(res.checkIn.format(DATE_FORMAT)).append(",");
                csv.append(res.checkOut.format(DATE_FORMAT)).append(",");
                csv.append(res.status).append(",");
                csv.append(String.format("%.2f", amount)).append("\n");
                totalAmount = totalAmount.add(amount);
            }
        }

        csv.append("total,").append(String.format("%.2f", totalAmount)).append("\n");

        return csv.toString();
    }
}
