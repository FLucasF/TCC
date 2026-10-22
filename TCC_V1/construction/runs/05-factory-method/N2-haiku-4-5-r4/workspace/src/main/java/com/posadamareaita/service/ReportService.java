package com.posadamareaita.service;

import com.posadamareaita.domain.Reservation;
import com.posadamareaita.domain.ReservationStatus;
import com.posadamareaita.domain.Room;
import com.posadamareaita.repository.ReservationRepository;
import com.posadamareaita.repository.RoomRepository;
import com.posadamareaita.util.FormattingUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {
    public static String generateOccupancyReport(LocalDate date) throws Exception {
        List<Reservation> reservations = ReservationRepository.findByDateAndStatus(date);
        List<Room> rooms = RoomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;
        for (Reservation res : reservations) {
            Room room = rooms.stream().filter(r -> r.getNumber() == res.getRoomNumber()).findFirst().orElse(null);
            if (room != null) {
                csv.append(room.getNumber()).append(",");
                csv.append(room.getType().getDisplayName()).append(",");
                csv.append(res.getId()).append(",");
                csv.append(FormattingUtil.formatDateISO(res.getCheckIn())).append(",");
                csv.append(FormattingUtil.formatDateISO(res.getCheckOut())).append("\n");
                occupiedCount++;
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(rooms.size()).append("\n");

        return csv.toString();
    }

    public static String generateRevenueReport(int year, int month) throws Exception {
        List<Reservation> reservations = ReservationRepository.findByMonthAndStatus(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation res : reservations) {
            BigDecimal amount = res.getAmountPaid();
            if (res.getRefund() != null) {
                amount = amount.subtract(res.getRefund());
            }

            csv.append(res.getId()).append(",");
            csv.append(res.getRoomNumber()).append(",");
            csv.append(FormattingUtil.formatDateISO(res.getCheckIn())).append(",");
            csv.append(FormattingUtil.formatDateISO(res.getCheckOut())).append(",");
            csv.append(res.getStatus().toString()).append(",");
            csv.append(String.format("%.2f", amount)).append("\n");

            total = total.add(amount);
        }

        csv.append("total,,,,").append(String.format("%.2f", total)).append("\n");

        return csv.toString();
    }
}
