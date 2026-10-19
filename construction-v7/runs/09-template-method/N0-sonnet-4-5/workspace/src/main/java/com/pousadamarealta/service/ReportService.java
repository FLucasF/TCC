package com.pousadamarealta.service;

import com.pousadamarealta.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final BigDecimal TOURISM_TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationService reservationService;
    private final RoomService roomService;

    public ReportService(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();

        csv.append("Pousada Maré Alta,occupancy,").append(date.format(DATE_FORMAT)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupiedReservations = reservationService.getAllReservations().stream()
                .filter(r -> r.getStatus() != ReservationStatus.PENDING)
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> isOccupiedOnDate(r, date))
                .sorted(Comparator.comparing(Reservation::getRoomNumber))
                .toList();

        for (Reservation reservation : occupiedReservations) {
            Room room = roomService.findByNumber(reservation.getRoomNumber()).orElseThrow();
            csv.append(String.format("%s,%s,%d,%s,%s\n",
                    reservation.getRoomNumber(),
                    room.getType(),
                    reservation.getId(),
                    reservation.getCheckIn().format(DATE_FORMAT),
                    reservation.getCheckOut().format(DATE_FORMAT)));
        }

        int totalRooms = roomService.getAllRooms().size();
        csv.append(String.format("total,%d/%d\n", occupiedReservations.size(), totalRooms));

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();

        csv.append("Pousada Maré Alta,revenue,").append(month.format(DateTimeFormatter.ofPattern("yyyy-MM"))).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> monthReservations = reservationService.getAllReservations().stream()
                .filter(r -> startsInMonth(r, month))
                .filter(r -> hasRevenue(r))
                .sorted(Comparator.comparing(Reservation::getCheckIn)
                        .thenComparing(Reservation::getId))
                .toList();

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation reservation : monthReservations) {
            BigDecimal revenue = calculateRevenue(reservation);
            total = total.add(revenue);

            csv.append(String.format("%d,%s,%s,%s,%s,%s\n",
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getCheckIn().format(DATE_FORMAT),
                    reservation.getCheckOut().format(DATE_FORMAT),
                    reservation.getStatus(),
                    formatDecimal(revenue)));
        }

        csv.append(String.format("total,%s\n", formatDecimal(total)));

        return csv.toString();
    }

    public String generateTourismTaxReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();

        csv.append("Pousada Maré Alta,tourism-tax,").append(month.format(DateTimeFormatter.ofPattern("yyyy-MM"))).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        List<Reservation> paidReservations = reservationService.getAllReservations().stream()
                .filter(r -> startsInMonth(r, month))
                .filter(r -> isPaid(r))
                .sorted(Comparator.comparing(Reservation::getCheckIn)
                        .thenComparing(Reservation::getId))
                .toList();

        BigDecimal totalTax = BigDecimal.ZERO;

        for (Reservation reservation : paidReservations) {
            BigDecimal tax = calculateTourismTax(reservation);
            totalTax = totalTax.add(tax);

            csv.append(String.format("%d,%s,%s,%s,%d,%d,%s\n",
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getCheckIn().format(DATE_FORMAT),
                    reservation.getCheckOut().format(DATE_FORMAT),
                    reservation.getGuests(),
                    reservation.getNights(),
                    formatDecimal(tax)));
        }

        csv.append(String.format("total,%s\n", formatDecimal(totalTax)));

        return csv.toString();
    }

    public String generateRatePlansReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();

        csv.append("Pousada Maré Alta,rate-plans,").append(month.format(DateTimeFormatter.ofPattern("yyyy-MM"))).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        Map<RatePlan, List<Reservation>> groupedByPlan = reservationService.getAllReservations().stream()
                .filter(r -> startsInMonth(r, month))
                .filter(r -> hasRevenue(r))
                .collect(Collectors.groupingBy(Reservation::getRatePlan));

        Map<RatePlan, BigDecimal> revenueByPlan = new EnumMap<>(RatePlan.class);
        Map<RatePlan, Integer> countByPlan = new EnumMap<>(RatePlan.class);

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> planReservations = groupedByPlan.getOrDefault(plan, Collections.emptyList());
            BigDecimal planRevenue = planReservations.stream()
                    .map(this::calculateRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            revenueByPlan.put(plan, planRevenue);
            countByPlan.put(plan, planReservations.size());
        }

        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (RatePlan plan : RatePlan.values()) {
            int count = countByPlan.get(plan);
            BigDecimal amount = revenueByPlan.get(plan);

            csv.append(String.format("%s,%d,%s\n",
                    plan,
                    count,
                    formatDecimal(amount)));

            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }

        csv.append(String.format("total,%d,%s\n", totalCount, formatDecimal(totalAmount)));

        return csv.toString();
    }

    private boolean isOccupiedOnDate(Reservation reservation, LocalDate date) {
        return !date.isBefore(reservation.getCheckIn()) && date.isBefore(reservation.getCheckOut());
    }

    private boolean startsInMonth(Reservation reservation, YearMonth month) {
        YearMonth checkInMonth = YearMonth.from(reservation.getCheckIn());
        return checkInMonth.equals(month);
    }

    private boolean isPaid(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.CONFIRMED ||
                reservation.getStatus() == ReservationStatus.CHECKED_IN ||
                reservation.getStatus() == ReservationStatus.CHECKED_OUT;
    }

    private boolean hasRevenue(Reservation reservation) {
        BigDecimal revenue = calculateRevenue(reservation);
        return revenue.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal calculateRevenue(Reservation reservation) {
        BigDecimal paid = reservation.getAmountPaid();
        BigDecimal refunded = reservation.getRefund() != null ? reservation.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }

    private BigDecimal calculateTourismTax(Reservation reservation) {
        int guestNights = reservation.getGuests() * reservation.getNights();
        return TOURISM_TAX_PER_GUEST_PER_NIGHT.multiply(new BigDecimal(guestNights));
    }

    private String formatDecimal(BigDecimal value) {
        return value.setScale(2).toPlainString();
    }
}
