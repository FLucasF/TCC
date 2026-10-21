package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportService {
    private final Repository repository;

    public ReportService(Repository repository) {
        this.repository = repository;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Reservation> occupiedReservations = repository.getAllReservations().stream()
                .filter(r -> r.status == Reservation.Status.CONFIRMED ||
                            r.status == Reservation.Status.CHECKED_IN ||
                            r.status == Reservation.Status.CHECKED_OUT)
                .filter(r -> !date.isBefore(r.checkIn) && date.isBefore(r.checkOut))
                .sorted(Comparator.comparingInt((Reservation r) -> r.roomNumber)
                       .thenComparingInt(r -> r.id))
                .collect(Collectors.toList());

        for (Reservation res : occupiedReservations) {
            Room room = repository.getRoom(res.roomNumber);
            csv.append(String.format("%d,%s,%d,%s,%s\n", res.roomNumber, room.type, res.id,
                                    res.checkIn.format(formatter), res.checkOut.format(formatter)));
        }

        int totalRooms = repository.getAllRooms().size();
        csv.append(String.format("total,%d/%d\n", occupiedReservations.size(), totalRooms));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Reservation> relevantReservations = repository.getAllReservations().stream()
                .filter(r -> YearMonth.from(r.checkIn).equals(month))
                .filter(r -> r.status == Reservation.Status.CONFIRMED ||
                            r.status == Reservation.Status.CHECKED_IN ||
                            r.status == Reservation.Status.CHECKED_OUT ||
                            r.status == Reservation.Status.CANCELLED)
                .sorted(Comparator.comparing((Reservation r) -> r.checkIn)
                       .thenComparingInt(r -> r.id))
                .collect(Collectors.toList());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Reservation res : relevantReservations) {
            BigDecimal amount = res.amountPaid;
            if (res.refund != null) {
                amount = amount.subtract(res.refund);
            }

            csv.append(String.format("%d,%d,%s,%s,%s,%.2f\n", res.id, res.roomNumber,
                                    res.checkIn.format(formatter), res.checkOut.format(formatter),
                                    res.status.name(), amount));
            totalAmount = totalAmount.add(amount);
        }

        csv.append(String.format("total,,,,%.2f\n", totalAmount));

        return csv.toString();
    }
}
