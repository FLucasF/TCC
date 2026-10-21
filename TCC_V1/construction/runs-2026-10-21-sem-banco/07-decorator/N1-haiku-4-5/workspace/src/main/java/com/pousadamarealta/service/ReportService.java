package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ReportService {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Room> rooms = roomService.getAllRooms().stream()
                .sorted(Comparator.comparingInt(Room::getNumber))
                .collect(Collectors.toList());

        int occupiedCount = 0;
        for (Room room : rooms) {
            List<Reservation> reservations = reservationService.getConfirmedReservationsByRoomAndDate(room.getNumber(), date);
            if (!reservations.isEmpty()) {
                for (Reservation res : reservations) {
                    csv.append(room.getNumber()).append(",");
                    csv.append(room.getType()).append(",");
                    csv.append(res.getId()).append(",");
                    csv.append(formatDate(res.getCheckIn())).append(",");
                    csv.append(formatDate(res.getCheckOut())).append("\n");
                    occupiedCount++;
                }
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(rooms.size());
        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> reservations = reservationService.getReservationsByMonthStarting(month.getYear(), month.getMonthValue())
                .stream()
                .sorted(Comparator.comparing(Reservation::getCheckIn)
                        .thenComparingLong(Reservation::getId))
                .collect(Collectors.toList());

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation res : reservations) {
            BigDecimal amount = calculateRevenueAmount(res);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                csv.append(res.getId()).append(",");
                csv.append(res.getRoomNumber()).append(",");
                csv.append(formatDate(res.getCheckIn())).append(",");
                csv.append(formatDate(res.getCheckOut())).append(",");
                csv.append(res.getStatus()).append(",");
                csv.append(formatMoneyForCsv(amount)).append("\n");
                total = total.add(amount);
            }
        }

        csv.append("total,").append(formatMoneyForCsv(total));
        return csv.toString();
    }

    private BigDecimal calculateRevenueAmount(Reservation res) {
        BigDecimal paid = res.getAmountPaid();
        BigDecimal refund = res.getRefund() != null ? res.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refund);
    }

    private String formatDate(LocalDate date) {
        return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
    }

    private String formatMoneyForCsv(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString();
    }
}
