package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.util.DateUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationService.getReservationsForOccupancyReport(date);

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : reservations) {
            Room room = roomService.getRoomByNumber(r.getRoomNumber()).orElseThrow();
            csv.append(String.format("%d,%s,%d,%s,%s\n",
                    r.getRoomNumber(),
                    room.getType(),
                    r.getId(),
                    DateUtils.formatForApi(r.getCheckIn()),
                    DateUtils.formatForApi(r.getCheckOut())));
        }

        int totalRooms = roomService.getAllRooms().size();
        csv.append(String.format("total,%d/%d", reservations.size(), totalRooms));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationService.getReservationsForRevenueReport(
                month.getYear(), month.getMonthValue());

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal amount = r.getAmountPaid();
            if (r.getRefund() != null) {
                amount = amount.subtract(r.getRefund());
            }

            total = total.add(amount);

            csv.append(String.format("%d,%d,%s,%s,%s,%.2f\n",
                    r.getId(),
                    r.getRoomNumber(),
                    DateUtils.formatForApi(r.getCheckIn()),
                    DateUtils.formatForApi(r.getCheckOut()),
                    r.getStatus(),
                    amount));
        }

        csv.append(String.format("total,,,,,%.2f", total));

        return csv.toString();
    }
}
