package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.util.DateHelper;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ReportService {
    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringWriter writer = new StringWriter();
        writer.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedReservations = new ArrayList<>();
        for (Reservation r : reservationService.getAllReservations()) {
            if (r.getStatus() == ReservationStatus.CONFIRMED ||
                r.getStatus() == ReservationStatus.CHECKED_IN ||
                r.getStatus() == ReservationStatus.CHECKED_OUT) {

                if (!date.isBefore(r.getCheckIn()) && date.isBefore(r.getCheckOut())) {
                    occupiedReservations.add(r);
                }
            }
        }

        occupiedReservations.sort((a, b) -> Integer.compare(a.getRoomNumber(), b.getRoomNumber()));

        for (Reservation r : occupiedReservations) {
            try {
                String roomType = roomService.getRoomByNumber(r.getRoomNumber()).getType();
                writer.append(String.valueOf(r.getRoomNumber())).append(",")
                        .append(roomType).append(",")
                        .append(String.valueOf(r.getId())).append(",")
                        .append(DateHelper.formatDateIso(r.getCheckIn())).append(",")
                        .append(DateHelper.formatDateIso(r.getCheckOut())).append("\n");
            } catch (Exception e) {
                // Skip room if not found
            }
        }

        int occupiedCount = occupiedReservations.size();
        int totalRooms = roomService.getAllRooms().size();
        writer.append("total,").append(String.valueOf(occupiedCount)).append("/").append(String.valueOf(totalRooms));

        return writer.toString();
    }

    public String generateRevenueReport(YearMonth yearMonth) {
        StringWriter writer = new StringWriter();
        writer.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> revenueReservations = new ArrayList<>();
        for (Reservation r : reservationService.getAllReservations()) {
            YearMonth reservationMonth = YearMonth.from(r.getCheckIn());
            if (reservationMonth.equals(yearMonth)) {
                if (r.getAmountPaid().compareTo(BigDecimal.ZERO) > 0 ||
                    (r.getRefund() != null && r.getRefund().compareTo(BigDecimal.ZERO) > 0)) {
                    revenueReservations.add(r);
                }
            }
        }

        revenueReservations.sort((a, b) -> {
            int dateCompare = a.getCheckIn().compareTo(b.getCheckIn());
            if (dateCompare != 0) return dateCompare;
            return Long.compare(a.getId(), b.getId());
        });

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation r : revenueReservations) {
            BigDecimal amount = r.getAmountPaid();
            if (r.getRefund() != null) {
                amount = amount.subtract(r.getRefund());
            }

            writer.append(String.valueOf(r.getId())).append(",")
                    .append(String.valueOf(r.getRoomNumber())).append(",")
                    .append(DateHelper.formatDateIso(r.getCheckIn())).append(",")
                    .append(DateHelper.formatDateIso(r.getCheckOut())).append(",")
                    .append(r.getStatus().toString()).append(",")
                    .append(formatCsvMoney(amount)).append("\n");

            total = total.add(amount);
        }

        writer.append("total,").append(formatCsvMoney(total));

        return writer.toString();
    }

    private String formatCsvMoney(BigDecimal value) {
        return String.format("%.2f", value).replace(",", ".");
    }
}
