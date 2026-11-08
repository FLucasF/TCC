package com.pousada.marealta.service;

import com.pousada.marealta.db.ReservationRepository;
import com.pousada.marealta.db.RoomRepository;
import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.util.Formatter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {
    public static String generateOccupancyReport(LocalDate date) throws Exception {
        List<Room> rooms = RoomRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;

        for (Room room : rooms) {
            var reservations = ReservationRepository.findByRoomOnDate(room.getNumber(), date);
            if (!reservations.isEmpty()) {
                for (Reservation res : reservations) {
                    csv.append(room.getNumber()).append(",")
                       .append(room.getType().displayName).append(",")
                       .append(res.getId()).append(",")
                       .append(Formatter.formatDateCSV(res.getCheckIn())).append(",")
                       .append(Formatter.formatDateCSV(res.getCheckOut())).append("\n");
                    occupiedCount++;
                }
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(rooms.size());

        return csv.toString();
    }

    public static String generateRevenueReport(YearMonth yearMonth) throws Exception {
        List<Reservation> reservations = ReservationRepository.findByMonthWithPayment(
            yearMonth.getYear(), yearMonth.getMonthValue()
        );

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal revenue = res.getAmountPaid();
            if (res.getRefund() != null) {
                revenue = revenue.subtract(res.getRefund());
            }

            csv.append(res.getId()).append(",")
               .append(res.getRoomNumber()).append(",")
               .append(Formatter.formatDateCSV(res.getCheckIn())).append(",")
               .append(Formatter.formatDateCSV(res.getCheckOut())).append(",")
               .append(res.getStatus().name()).append(",")
               .append(Formatter.formatMoneyCSV(revenue)).append("\n");

            total = total.add(revenue);
        }

        csv.append("total,,,,,").append(Formatter.formatMoneyCSV(total));

        return csv.toString();
    }
}
