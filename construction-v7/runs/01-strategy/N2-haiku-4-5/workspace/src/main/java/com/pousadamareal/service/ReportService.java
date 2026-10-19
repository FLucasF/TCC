package com.pousadamareal.service;

import com.pousadamareal.domain.Reservation;
import com.pousadamareal.domain.Room;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportService {
    private final DataStore dataStore;
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter ISO_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    public ReportService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Room> sortedRooms = dataStore.getAllRooms().stream()
                .sorted(Comparator.comparingInt(r -> r.number))
                .toList();

        int occupiedCount = 0;
        for (Room room : sortedRooms) {
            Optional<Reservation> occupant = findOccupantOnDate(room.number, date);
            if (occupant.isPresent()) {
                Reservation res = occupant.get();
                csv.append(res.roomNumber).append(",")
                        .append(room.type.name()).append(",")
                        .append(res.id).append(",")
                        .append(res.checkIn.format(ISO_DATE_FORMAT)).append(",")
                        .append(res.checkOut.format(ISO_DATE_FORMAT)).append("\n");
                occupiedCount++;
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(sortedRooms.size()).append("\n");

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> relevantReservations = dataStore.getAllReservations().stream()
                .filter(r -> YearMonth.from(r.checkIn).equals(month))
                .filter(r -> r.amountPaidCents > 0)
                .sorted(Comparator.comparing((Reservation r) -> r.checkIn)
                        .thenComparingLong(r -> r.id))
                .toList();

        long totalAmount = 0;
        for (Reservation res : relevantReservations) {
            long netAmount = res.amountPaidCents;
            if (res.refundCents != null) {
                netAmount -= res.refundCents;
            }

            csv.append(res.id).append(",")
                    .append(res.roomNumber).append(",")
                    .append(res.checkIn.format(ISO_DATE_FORMAT)).append(",")
                    .append(res.checkOut.format(ISO_DATE_FORMAT)).append(",")
                    .append(res.status.name()).append(",")
                    .append(formatCurrency(netAmount)).append("\n");

            totalAmount += netAmount;
        }

        csv.append("total,,,,").append(formatCurrency(totalAmount)).append("\n");

        return csv.toString();
    }

    private Optional<Reservation> findOccupantOnDate(int roomNumber, LocalDate date) {
        return dataStore.getAllReservations().stream()
                .filter(r -> r.roomNumber == roomNumber)
                .filter(r -> r.status != Reservation.ReservationStatus.CANCELLED)
                .filter(r -> isOccupantOnDate(r, date))
                .findFirst();
    }

    private boolean isOccupantOnDate(Reservation r, LocalDate date) {
        return !r.checkIn.isAfter(date) && r.checkOut.isAfter(date);
    }

    private String formatCurrency(long cents) {
        return String.format("%.2f", cents / 100.0);
    }
}
