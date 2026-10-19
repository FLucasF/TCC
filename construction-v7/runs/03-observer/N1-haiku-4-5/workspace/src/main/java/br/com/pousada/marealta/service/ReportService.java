package br.com.pousada.marealta.service;

import br.com.pousada.marealta.model.*;
import br.com.pousada.marealta.util.DateUtil;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ReportService {
    private final ReservationService reservationService;

    public ReportService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public String getOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedReservations = new ArrayList<>();
        for (Reservation res : reservationService.getAllReservations()) {
            if (res.getStatus() == ReservationStatus.CANCELLED) {
                continue;
            }
            if (res.getStatus() == ReservationStatus.PENDING) {
                continue;
            }

            LocalDate checkInDate = res.getCheckIn();
            LocalDate checkOutDate = res.getCheckOut();

            if (!date.isBefore(checkInDate) && date.isBefore(checkOutDate)) {
                occupiedReservations.add(res);
            }
        }

        occupiedReservations.sort(Comparator.comparingInt(Reservation::getRoomNumber)
                .thenComparingLong(Reservation::getId));

        for (Reservation res : occupiedReservations) {
            Room room = reservationService.getRoom(res.getRoomNumber());
            csv.append(res.getRoomNumber()).append(",")
                    .append(room.type().getDisplayName()).append(",")
                    .append(res.getId()).append(",")
                    .append(DateUtil.formatForApi(res.getCheckIn())).append(",")
                    .append(DateUtil.formatForApi(res.getCheckOut())).append("\n");
        }

        csv.append("total,").append(occupiedReservations.size()).append("/5\n");
        return csv.toString();
    }

    public String getRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> monthReservations = new ArrayList<>();
        for (Reservation res : reservationService.getAllReservations()) {
            LocalDate checkInMonth = res.getCheckIn();
            if (YearMonth.from(checkInMonth).equals(month)) {
                if (isMoneyReceivedOrRefunded(res)) {
                    monthReservations.add(res);
                }
            }
        }

        monthReservations.sort(Comparator.comparing(Reservation::getCheckIn)
                .thenComparingLong(Reservation::getId));

        double totalAmount = 0;
        for (Reservation res : monthReservations) {
            double amount = res.getAmountPaid();
            if (res.getRefund() != null) {
                amount -= res.getRefund();
            }

            csv.append(res.getId()).append(",")
                    .append(res.getRoomNumber()).append(",")
                    .append(DateUtil.formatForApi(res.getCheckIn())).append(",")
                    .append(DateUtil.formatForApi(res.getCheckOut())).append(",")
                    .append(res.getStatus()).append(",")
                    .append(String.format("%.2f", amount)).append("\n");

            totalAmount += amount;
        }

        csv.append("total,,,,").append(String.format("%.2f", totalAmount)).append("\n");
        return csv.toString();
    }

    private boolean isMoneyReceivedOrRefunded(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.CONFIRMED ||
                reservation.getStatus() == ReservationStatus.CHECKED_IN ||
                reservation.getStatus() == ReservationStatus.CHECKED_OUT ||
                (reservation.getStatus() == ReservationStatus.CANCELLED && reservation.getRefund() != null);
    }
}
