package com.pousada.service;

import com.pousada.model.Reservation;
import com.pousada.model.ReservationStatus;
import com.pousada.model.Room;
import com.pousada.model.RatePlan;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,occupancy,").append(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedRooms = 0;

        var sortedRooms = roomRepository.findAll().stream()
            .sorted(Comparator.comparingInt(Room::getNumber))
            .collect(Collectors.toList());

        for (Room room : sortedRooms) {
            var reservations = reservationRepository.findAll().stream()
                .filter(r -> r.getRoomNumber() == room.getNumber())
                .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
                .filter(r -> isReservationOnDate(r, date))
                .collect(Collectors.toList());

            if (!reservations.isEmpty()) {
                for (Reservation r : reservations) {
                    csv.append(room.getNumber()).append(",")
                        .append(room.getType()).append(",")
                        .append(r.getId()).append(",")
                        .append(r.getCheckIn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append(",")
                        .append(r.getCheckOut().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
                    occupiedRooms++;
                }
            }
        }

        csv.append("total,").append(occupiedRooms).append("/").append(roomRepository.findAll().size()).append("\n");

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,revenue,").append(month.format(DateTimeFormatter.ofPattern("yyyy-MM"))).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;

        var reservations = reservationRepository.findByMonth(month.getYear(), month.getMonthValue()).stream()
            .filter(r -> r.getStatus() != ReservationStatus.CANCELLED || r.getStatus() == ReservationStatus.CANCELLED)
            .sorted(Comparator.comparing(Reservation::getCheckIn).thenComparingLong(Reservation::getId))
            .collect(Collectors.toList());

        for (Reservation r : reservations) {
            if (r.getStatus() != ReservationStatus.PENDING && r.getStatus() != ReservationStatus.CANCELLED) {
                csv.append(r.getId()).append(",")
                    .append(r.getRoomNumber()).append(",")
                    .append(r.getCheckIn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append(",")
                    .append(r.getCheckOut().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append(",")
                    .append(r.getStatus()).append(",")
                    .append(formatAmount(r.getAmountPaid().subtract(r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO))).append("\n");

                totalAmount = totalAmount.add(r.getAmountPaid().subtract(r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO));
            } else if (r.getStatus() == ReservationStatus.CANCELLED && r.getRatePlan() == RatePlan.NON_REFUNDABLE) {
                // Non-refundable cancelled reservations still count
                csv.append(r.getId()).append(",")
                    .append(r.getRoomNumber()).append(",")
                    .append(r.getCheckIn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append(",")
                    .append(r.getCheckOut().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append(",")
                    .append(r.getStatus()).append(",")
                    .append(formatAmount(r.getAmountPaid())).append("\n");

                totalAmount = totalAmount.add(r.getAmountPaid());
            }
        }

        csv.append("total,,,,").append(formatAmount(totalAmount)).append("\n");

        return csv.toString();
    }

    public String generateTourismTaxReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,tourism-tax,").append(month.format(DateTimeFormatter.ofPattern("yyyy-MM"))).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal totalTax = BigDecimal.ZERO;
        final BigDecimal TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

        var reservations = reservationRepository.findByMonth(month.getYear(), month.getMonthValue()).stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.CHECKED_IN || r.getStatus() == ReservationStatus.CHECKED_OUT)
            .sorted(Comparator.comparing(Reservation::getCheckIn).thenComparingLong(Reservation::getId))
            .collect(Collectors.toList());

        for (Reservation r : reservations) {
            BigDecimal tax = TAX_PER_GUEST_PER_NIGHT
                .multiply(BigDecimal.valueOf(r.getGuests()))
                .multiply(BigDecimal.valueOf(r.getNights()));

            csv.append(r.getId()).append(",")
                .append(r.getRoomNumber()).append(",")
                .append(r.getCheckIn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append(",")
                .append(r.getCheckOut().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append(",")
                .append(r.getGuests()).append(",")
                .append(r.getNights()).append(",")
                .append(formatAmount(tax)).append("\n");

            totalTax = totalTax.add(tax);
        }

        csv.append("total,,,,,").append(formatAmount(totalTax)).append("\n");

        return csv.toString();
    }

    public String generateRatePlansReport(YearMonth month) {
        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month.format(DateTimeFormatter.ofPattern("yyyy-MM"))).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        var allReservations = reservationRepository.findByMonth(month.getYear(), month.getMonthValue());

        Map<RatePlan, BigDecimal> amountByPlan = new HashMap<>();
        Map<RatePlan, Integer> countByPlan = new HashMap<>();

        for (RatePlan plan : RatePlan.values()) {
            amountByPlan.put(plan, BigDecimal.ZERO);
            countByPlan.put(plan, 0);
        }

        for (Reservation r : allReservations) {
            // Include paid reservations and cancelled non-refundable
            if (r.getStatus() != ReservationStatus.PENDING && r.getStatus() != ReservationStatus.CANCELLED) {
                BigDecimal amount = r.getAmountPaid().subtract(r.getRefund() != null ? r.getRefund() : BigDecimal.ZERO);
                amountByPlan.put(r.getRatePlan(), amountByPlan.get(r.getRatePlan()).add(amount));
                countByPlan.put(r.getRatePlan(), countByPlan.get(r.getRatePlan()) + 1);
            } else if (r.getStatus() == ReservationStatus.CANCELLED && r.getRatePlan() == RatePlan.NON_REFUNDABLE) {
                // Non-refundable cancelled reservations still count
                amountByPlan.put(r.getRatePlan(), amountByPlan.get(r.getRatePlan()).add(r.getAmountPaid()));
                countByPlan.put(r.getRatePlan(), countByPlan.get(r.getRatePlan()) + 1);
            }
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalCount = 0;

        for (RatePlan plan : RatePlan.values()) {
            csv.append(plan).append(",")
                .append(countByPlan.get(plan)).append(",")
                .append(formatAmount(amountByPlan.get(plan))).append("\n");

            totalAmount = totalAmount.add(amountByPlan.get(plan));
            totalCount += countByPlan.get(plan);
        }

        csv.append("total,").append(totalCount).append(",").append(formatAmount(totalAmount)).append("\n");

        return csv.toString();
    }

    private boolean isReservationOnDate(Reservation reservation, LocalDate date) {
        return !reservation.getCheckIn().isAfter(date) && date.isBefore(reservation.getCheckOut());
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%.2f", amount);
    }
}
