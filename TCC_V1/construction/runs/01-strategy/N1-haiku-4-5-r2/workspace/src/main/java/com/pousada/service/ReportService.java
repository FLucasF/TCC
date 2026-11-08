package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.domain.ReservationStatus;
import com.pousada.infra.ReservationRepository;
import com.pousada.infra.RoomRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) throws SQLException {
        List<Room> allRooms = roomRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;

        for (Room room : allRooms) {
            var reservations = reservationRepository.findByRoomAndDateRange(room.getNumber(), date, date.plusDays(1));

            boolean occupied = false;
            for (Reservation res : reservations) {
                if (res.getStatus() == ReservationStatus.CONFIRMED ||
                    res.getStatus() == ReservationStatus.CHECKED_IN ||
                    res.getStatus() == ReservationStatus.CHECKED_OUT) {

                    csv.append(room.getNumber()).append(",")
                       .append(room.getType()).append(",")
                       .append(res.getId()).append(",")
                       .append(formatDate(res.getCheckIn())).append(",")
                       .append(formatDate(res.getCheckOut())).append("\n");
                    occupied = true;
                }
            }

            if (occupied) {
                occupiedCount++;
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(allRooms.size()).append("\n");
        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) throws SQLException {
        List<Reservation> reservations = reservationRepository.findByMonthAndPaid(month.getYear(), month.getMonthValue());

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amount = res.getAmountPaid();
            if (res.getRefund() != null) {
                amount = amount.subtract(res.getRefund());
            }

            csv.append(res.getId()).append(",")
               .append(res.getRoomNumber()).append(",")
               .append(formatDateForCsv(res.getCheckIn())).append(",")
               .append(formatDateForCsv(res.getCheckOut())).append(",")
               .append(res.getStatus()).append(",")
               .append(formatMoneyForCsv(amount)).append("\n");

            total = total.add(amount);
        }

        csv.append("total,").append(formatMoneyForCsv(total)).append("\n");
        return csv.toString();
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private String formatDateForCsv(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private String formatMoneyForCsv(BigDecimal amount) {
        return String.format("%.2f", amount);
    }
}
