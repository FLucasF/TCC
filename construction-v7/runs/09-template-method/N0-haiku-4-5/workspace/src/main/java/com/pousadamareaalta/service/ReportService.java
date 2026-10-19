package com.pousadamareaalta.service;

import com.pousadamareaalta.model.Reservation;
import com.pousadamareaalta.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

public class ReportService {
    private final ReservationService reservationService;
    private static final BigDecimal TOURISM_TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    public ReportService(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();

        csv.append("Pousada Maré Alta,occupancy,").append(formatDateForTitle(date)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        List<Reservation> occupied = getOccupiedReservationsForDate(date);
        int occupiedRooms = 0;

        for (Reservation res : occupied) {
            Room room = reservationService.getRoom(res.getRoomNumber());
            csv.append(room.getNumber()).append(",")
                    .append(room.getType()).append(",")
                    .append(res.getId()).append(",")
                    .append(formatDate(res.getCheckIn())).append(",")
                    .append(formatDate(res.getCheckOut())).append("\n");
            occupiedRooms++;
        }

        csv.append("total,").append(occupiedRooms).append("/").append(reservationService.getRooms().size()).append("\n");

        return csv.toString();
    }

    private List<Reservation> getOccupiedReservationsForDate(LocalDate date) {
        return reservationService.getReservations().values().stream()
                .filter(res -> res.getStatus() != Reservation.Status.CANCELLED)
                .filter(res -> isNightOccupied(res, date))
                .sorted(Comparator.comparingInt(Reservation::getRoomNumber))
                .collect(Collectors.toList());
    }

    private boolean isNightOccupied(Reservation res, LocalDate date) {
        return !date.isBefore(res.getCheckIn()) && date.isBefore(res.getCheckOut());
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();

        csv.append("Pousada Maré Alta,revenue,").append(month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> monthReservations = reservationService.getReservations().values().stream()
                .filter(res -> res.getCheckIn().getYear() == month.getYear() &&
                              res.getCheckIn().getMonthValue() == month.getMonthValue())
                .filter(res -> isPaidReservation(res))
                .sorted(Comparator.comparingInt((Reservation r) -> r.getCheckIn().getDayOfMonth())
                        .thenComparingLong(Reservation::getId))
                .collect(Collectors.toList());

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation res : monthReservations) {
            BigDecimal amount = res.getAmountPaid().subtract(res.getRefund() != null ? res.getRefund() : BigDecimal.ZERO);
            csv.append(res.getId()).append(",")
                    .append(res.getRoomNumber()).append(",")
                    .append(formatDate(res.getCheckIn())).append(",")
                    .append(formatDate(res.getCheckOut())).append(",")
                    .append(res.getStatus()).append(",")
                    .append(reservationService.formatMoneyForCsv(amount)).append("\n");
            total = total.add(amount);
        }

        csv.append("total,,,,").append(reservationService.formatMoneyForCsv(total)).append("\n");

        return csv.toString();
    }

    public String generateTourismTaxReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();

        csv.append("Pousada Maré Alta,tourism-tax,").append(month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        List<Reservation> monthReservations = reservationService.getReservations().values().stream()
                .filter(res -> res.getCheckIn().getYear() == month.getYear() &&
                              res.getCheckIn().getMonthValue() == month.getMonthValue())
                .filter(res -> isPaidReservation(res))
                .sorted(Comparator.comparingInt((Reservation r) -> r.getCheckIn().getDayOfMonth())
                        .thenComparingLong(Reservation::getId))
                .collect(Collectors.toList());

        BigDecimal totalTax = BigDecimal.ZERO;
        for (Reservation res : monthReservations) {
            BigDecimal tax = TOURISM_TAX_PER_GUEST_PER_NIGHT
                    .multiply(new BigDecimal(res.getGuests()))
                    .multiply(new BigDecimal(res.getNights()));
            tax = tax.setScale(2, java.math.RoundingMode.HALF_EVEN);
            csv.append(res.getId()).append(",")
                    .append(res.getRoomNumber()).append(",")
                    .append(formatDate(res.getCheckIn())).append(",")
                    .append(formatDate(res.getCheckOut())).append(",")
                    .append(res.getGuests()).append(",")
                    .append(res.getNights()).append(",")
                    .append(reservationService.formatMoneyForCsv(tax)).append("\n");
            totalTax = totalTax.add(tax);
        }

        csv.append("total,,,,,").append(reservationService.formatMoneyForCsv(totalTax)).append("\n");

        return csv.toString();
    }

    public String generateRatePlansReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();

        csv.append("Pousada Maré Alta,rate-plans,").append(month.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        Map<Reservation.RatePlan, Long> reservationCounts = new EnumMap<>(Reservation.RatePlan.class);
        Map<Reservation.RatePlan, BigDecimal> amounts = new EnumMap<>(Reservation.RatePlan.class);

        for (Reservation.RatePlan plan : Reservation.RatePlan.values()) {
            reservationCounts.put(plan, 0L);
            amounts.put(plan, BigDecimal.ZERO);
        }

        List<Reservation> monthReservations = reservationService.getReservations().values().stream()
                .filter(res -> res.getCheckIn().getYear() == month.getYear() &&
                              res.getCheckIn().getMonthValue() == month.getMonthValue())
                .filter(res -> isPaidReservation(res))
                .collect(Collectors.toList());

        for (Reservation res : monthReservations) {
            reservationCounts.put(res.getRatePlan(), reservationCounts.get(res.getRatePlan()) + 1);
            BigDecimal amount = res.getAmountPaid().subtract(res.getRefund() != null ? res.getRefund() : BigDecimal.ZERO);
            amounts.put(res.getRatePlan(), amounts.get(res.getRatePlan()).add(amount));
        }

        long totalReservations = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Reservation.RatePlan plan : Reservation.RatePlan.values()) {
            csv.append(plan).append(",")
                    .append(reservationCounts.get(plan)).append(",")
                    .append(reservationService.formatMoneyForCsv(amounts.get(plan))).append("\n");
            totalReservations += reservationCounts.get(plan);
            totalAmount = totalAmount.add(amounts.get(plan));
        }

        csv.append("total,").append(totalReservations).append(",").append(reservationService.formatMoneyForCsv(totalAmount)).append("\n");

        return csv.toString();
    }

    private boolean isPaidReservation(Reservation res) {
        return res.getStatus() == Reservation.Status.CONFIRMED ||
               res.getStatus() == Reservation.Status.CHECKED_IN ||
               res.getStatus() == Reservation.Status.CHECKED_OUT;
    }

    private String formatDate(LocalDate date) {
        return date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String formatDateForTitle(LocalDate date) {
        return date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
