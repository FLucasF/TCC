package com.pousadamarealta.service;

import com.pousadamarealta.model.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final BigDecimal TOURISM_TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<ReservationStatus> paidStatuses = List.of(
                ReservationStatus.CONFIRMED,
                ReservationStatus.CHECKED_IN,
                ReservationStatus.CHECKED_OUT
        );

        List<Reservation> reservations = reservationRepository.findByDateAndStatus(date, paidStatuses);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,occupancy,").append(date.format(DATE_FORMATTER)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : reservations) {
            Room room = allRooms.stream()
                    .filter(rm -> rm.getNumber().equals(r.getRoomNumber()))
                    .findFirst()
                    .orElseThrow();

            csv.append(r.getRoomNumber()).append(",");
            csv.append(room.getType().name()).append(",");
            csv.append(r.getId()).append(",");
            csv.append(r.getCheckIn().format(DATE_FORMATTER)).append(",");
            csv.append(r.getCheckOut().format(DATE_FORMATTER)).append("\n");
        }

        csv.append("total,").append(reservations.size()).append("/").append(allRooms.size());
        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> allReservations = reservationRepository.findByMonth(year, month);

        List<Reservation> revenueReservations = allReservations.stream()
                .filter(r -> r.getStatus() != ReservationStatus.PENDING)
                .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,revenue,").append(String.format("%04d-%02d", year, month)).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Reservation r : revenueReservations) {
            BigDecimal revenue = calculateRevenue(r);
            totalRevenue = totalRevenue.add(revenue);

            csv.append(r.getId()).append(",");
            csv.append(r.getRoomNumber()).append(",");
            csv.append(r.getCheckIn().format(DATE_FORMATTER)).append(",");
            csv.append(r.getCheckOut().format(DATE_FORMATTER)).append(",");
            csv.append(r.getStatus().name()).append(",");
            csv.append(formatAmount(revenue)).append("\n");
        }

        csv.append("total,").append(formatAmount(totalRevenue));
        return csv.toString();
    }

    public String generateTourismTaxReport(int year, int month) {
        List<Reservation> allReservations = reservationRepository.findByMonth(year, month);

        List<Reservation> paidReservations = allReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                             r.getStatus() == ReservationStatus.CHECKED_IN ||
                             r.getStatus() == ReservationStatus.CHECKED_OUT)
                .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,tourism-tax,").append(String.format("%04d-%02d", year, month)).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal totalTax = BigDecimal.ZERO;

        for (Reservation r : paidReservations) {
            BigDecimal tax = calculateTourismTax(r);
            totalTax = totalTax.add(tax);

            csv.append(r.getId()).append(",");
            csv.append(r.getRoomNumber()).append(",");
            csv.append(r.getCheckIn().format(DATE_FORMATTER)).append(",");
            csv.append(r.getCheckOut().format(DATE_FORMATTER)).append(",");
            csv.append(r.getGuests()).append(",");
            csv.append(r.getNights()).append(",");
            csv.append(formatAmount(tax)).append("\n");
        }

        csv.append("total,").append(formatAmount(totalTax));
        return csv.toString();
    }

    public String generateRatePlansReport(int year, int month) {
        List<Reservation> allReservations = reservationRepository.findByMonth(year, month);

        List<Reservation> revenueReservations = allReservations.stream()
                .filter(r -> r.getStatus() != ReservationStatus.PENDING)
                .collect(Collectors.toList());

        Map<RatePlan, List<Reservation>> byPlan = revenueReservations.stream()
                .collect(Collectors.groupingBy(Reservation::getRatePlan));

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(String.format("%04d-%02d", year, month)).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (RatePlan plan : RatePlan.values()) {
            List<Reservation> planReservations = byPlan.getOrDefault(plan, Collections.emptyList());
            int count = planReservations.size();
            BigDecimal amount = planReservations.stream()
                    .map(this::calculateRevenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            csv.append(plan.name()).append(",");
            csv.append(count).append(",");
            csv.append(formatAmount(amount)).append("\n");

            totalCount += count;
            totalAmount = totalAmount.add(amount);
        }

        csv.append("total,").append(totalCount).append(",").append(formatAmount(totalAmount));
        return csv.toString();
    }

    private BigDecimal calculateRevenue(Reservation r) {
        BigDecimal paid = r.getAmountPaid();
        BigDecimal refunded = r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO;
        return paid.subtract(refunded);
    }

    private BigDecimal calculateTourismTax(Reservation r) {
        return TOURISM_TAX_PER_GUEST_PER_NIGHT
                .multiply(BigDecimal.valueOf(r.getGuests()))
                .multiply(BigDecimal.valueOf(r.getNights()));
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString();
    }
}
