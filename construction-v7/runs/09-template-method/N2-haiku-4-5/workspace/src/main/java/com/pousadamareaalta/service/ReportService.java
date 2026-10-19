package com.pousadamareaalta.service;

import com.pousadamareaalta.model.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class ReportService {
    private final RoomService roomService;
    private final ReservationService reservationService;
    private static final BigDecimal TOURISM_TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    public ReportService(RoomService roomService, ReservationService reservationService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> occupiedReservations = getOccupiedReservations(date);

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,occupancy,").append(formatDate(date)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation res : occupiedReservations) {
            Room room = roomService.getRoom(res.getRoomNumber());
            csv.append(res.getRoomNumber()).append(",");
            csv.append(room.getType()).append(",");
            csv.append(res.getId()).append(",");
            csv.append(formatDate(res.getCheckIn())).append(",");
            csv.append(formatDate(res.getCheckOut())).append("\n");
        }

        csv.append("total,").append(occupiedReservations.size()).append("/5\n");
        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> monthReservations = getMonthReservations(month);

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,revenue,").append(formatMonth(month)).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation res : monthReservations) {
            BigDecimal amount = res.getAmountPaid().subtract(res.getRefund() != null ? res.getRefund() : BigDecimal.ZERO);
            csv.append(res.getId()).append(",");
            csv.append(res.getRoomNumber()).append(",");
            csv.append(formatDate(res.getCheckIn())).append(",");
            csv.append(formatDate(res.getCheckOut())).append(",");
            csv.append(res.getStatus()).append(",");
            csv.append(formatMoney(amount)).append("\n");
            total = total.add(amount);
        }

        csv.append("total,,,,").append(formatMoney(total)).append("\n");
        return csv.toString();
    }

    public String generateTourismTaxReport(YearMonth month) {
        List<Reservation> monthReservations = getPaidReservationsInMonth(month);

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,tourism-tax,").append(formatMonth(month)).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation res : monthReservations) {
            BigDecimal tax = TOURISM_TAX_PER_GUEST_PER_NIGHT
                .multiply(BigDecimal.valueOf(res.getGuests()))
                .multiply(BigDecimal.valueOf(res.getNights()));

            csv.append(res.getId()).append(",");
            csv.append(res.getRoomNumber()).append(",");
            csv.append(formatDate(res.getCheckIn())).append(",");
            csv.append(formatDate(res.getCheckOut())).append(",");
            csv.append(res.getGuests()).append(",");
            csv.append(res.getNights()).append(",");
            csv.append(formatMoney(tax)).append("\n");
            total = total.add(tax);
        }

        csv.append("total,,,,,").append(formatMoney(total)).append("\n");
        return csv.toString();
    }

    public String generateRatePlansReport(YearMonth month) {
        List<Reservation> monthReservations = getMonthReservations(month);

        Map<RatePlan, Integer> counts = new LinkedHashMap<>();
        Map<RatePlan, BigDecimal> amounts = new LinkedHashMap<>();
        counts.put(RatePlan.STANDARD, 0);
        counts.put(RatePlan.NON_REFUNDABLE, 0);
        amounts.put(RatePlan.STANDARD, BigDecimal.ZERO);
        amounts.put(RatePlan.NON_REFUNDABLE, BigDecimal.ZERO);

        for (Reservation res : monthReservations) {
            RatePlan plan = res.getRatePlan();
            counts.put(plan, counts.get(plan) + 1);
            BigDecimal amount = res.getAmountPaid().subtract(res.getRefund() != null ? res.getRefund() : BigDecimal.ZERO);
            amounts.put(plan, amounts.get(plan).add(amount));
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(formatMonth(month)).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalReservations = 0;

        for (RatePlan plan : counts.keySet()) {
            csv.append(plan).append(",");
            csv.append(counts.get(plan)).append(",");
            csv.append(formatMoney(amounts.get(plan))).append("\n");
            totalReservations += counts.get(plan);
            totalAmount = totalAmount.add(amounts.get(plan));
        }

        csv.append("total,").append(totalReservations).append(",").append(formatMoney(totalAmount)).append("\n");
        return csv.toString();
    }

    private List<Reservation> getOccupiedReservations(LocalDate date) {
        return reservationService.getAllReservations().stream()
            .filter(res -> res.getStatus() != ReservationStatus.CANCELLED)
            .filter(res -> {
                LocalDate nextDay = date.plusDays(1);
                return !res.getCheckIn().isAfter(date) && res.getCheckOut().isAfter(date);
            })
            .sorted(Comparator.comparingInt(Reservation::getRoomNumber).thenComparingLong(Reservation::getId))
            .collect(Collectors.toList());
    }

    private List<Reservation> getMonthReservations(YearMonth month) {
        return reservationService.getAllReservations().stream()
            .filter(res -> YearMonth.from(res.getCheckIn()).equals(month))
            .sorted(Comparator.comparing(Reservation::getCheckIn).thenComparingLong(Reservation::getId))
            .collect(Collectors.toList());
    }

    private List<Reservation> getPaidReservationsInMonth(YearMonth month) {
        return reservationService.getAllReservations().stream()
            .filter(res -> YearMonth.from(res.getCheckIn()).equals(month))
            .filter(res -> res.getStatus() != ReservationStatus.PENDING && res.getStatus() != ReservationStatus.CANCELLED)
            .sorted(Comparator.comparing(Reservation::getCheckIn).thenComparingLong(Reservation::getId))
            .collect(Collectors.toList());
    }

    private String formatDate(LocalDate date) {
        return date.toString();
    }

    private String formatMonth(YearMonth month) {
        return month.toString();
    }

    private String formatMoney(BigDecimal amount) {
        return String.format("%.2f", amount);
    }
}
