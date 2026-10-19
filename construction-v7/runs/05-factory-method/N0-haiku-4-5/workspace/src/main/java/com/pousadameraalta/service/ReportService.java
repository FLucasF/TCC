package com.pousadameraalta.service;

import com.pousadameraalta.model.Reservation;
import com.pousadameraalta.model.ReservationStatus;
import com.pousadameraalta.repository.DataStore;
import com.pousadameraalta.util.Formatter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ReportService {
    private final DataStore dataStore = DataStore.getInstance();

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedRooms = new ArrayList<>();

        dataStore.getAllReservations().stream()
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                           r.getStatus() == ReservationStatus.CHECKED_IN ||
                           r.getStatus() == ReservationStatus.CHECKED_OUT)
                .filter(r -> {
                    LocalDate checkIn = r.getCheckIn();
                    LocalDate checkOut = r.getCheckOut();
                    return !date.isBefore(checkIn) && date.isBefore(checkOut);
                })
                .sorted(Comparator.comparingInt(Reservation::getRoomNumber))
                .forEach(occupiedRooms::add);

        for (Reservation r : occupiedRooms) {
            csv.append(r.getRoomNumber()).append(",")
               .append(getQuartType(r.getRoomNumber())).append(",")
               .append(r.getId()).append(",")
               .append(Formatter.formatIsoDate(r.getCheckIn())).append(",")
               .append(Formatter.formatIsoDate(r.getCheckOut())).append("\n");
        }

        int totalRooms = dataStore.getAllRooms().size();
        csv.append("total,").append(occupiedRooms.size()).append("/").append(totalRooms).append("\n");

        return csv.toString();
    }

    public String generateRevenueReport(String month) throws Exception {
        YearMonth yearMonth = parseYearMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;

        List<Reservation> reservations = new ArrayList<>();

        dataStore.getAllReservations().stream()
                .filter(r -> r.getCheckIn().getYear() == yearMonth.year &&
                           r.getCheckIn().getMonthValue() == yearMonth.month)
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                           r.getStatus() == ReservationStatus.CHECKED_IN ||
                           r.getStatus() == ReservationStatus.CHECKED_OUT ||
                           r.getStatus() == ReservationStatus.CANCELLED)
                .sorted(Comparator.comparing(Reservation::getCheckIn)
                               .thenComparingLong(Reservation::getId))
                .forEach(reservations::add);

        for (Reservation r : reservations) {
            BigDecimal amount = r.getAmountPaid();
            if (r.getRefund() != null) {
                amount = amount.subtract(r.getRefund());
            }

            if (amount.signum() > 0 || (r.getStatus() == ReservationStatus.CANCELLED && r.getAmountPaid().signum() > 0)) {
                csv.append(r.getId()).append(",")
                   .append(r.getRoomNumber()).append(",")
                   .append(Formatter.formatIsoDate(r.getCheckIn())).append(",")
                   .append(Formatter.formatIsoDate(r.getCheckOut())).append(",")
                   .append(r.getStatus()).append(",")
                   .append(Formatter.formatBrlCsv(amount)).append("\n");

                totalAmount = totalAmount.add(amount);
            }
        }

        csv.append("total,,,,").append(Formatter.formatBrlCsv(totalAmount)).append("\n");

        return csv.toString();
    }

    private String getQuartType(int roomNumber) {
        return switch (roomNumber) {
            case 101, 102 -> "STANDARD";
            case 201, 202 -> "DELUXE";
            case 301 -> "SUITE";
            default -> "UNKNOWN";
        };
    }

    private YearMonth parseYearMonth(String month) throws Exception {
        try {
            String[] parts = month.split("-");
            if (parts.length != 2) throw new NumberFormatException();
            int year = Integer.parseInt(parts[0]);
            int monthVal = Integer.parseInt(parts[1]);
            if (monthVal < 1 || monthVal > 12) throw new NumberFormatException();
            return new YearMonth(year, monthVal);
        } catch (Exception e) {
            throw new Exception("INVALID_REQUEST");
        }
    }

    private static class YearMonth {
        int year;
        int month;

        YearMonth(int year, int month) {
            this.year = year;
            this.month = month;
        }
    }
}
