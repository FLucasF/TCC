package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.RatePlan;
import com.pousada.domain.Room;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportService {
    private static final String POUSADA_NAME = "Pousada Maré Alta";
    private static final BigDecimal TOURISM_TAX_PER_GUEST_NIGHT = new BigDecimal("4.50");

    private final ReservationRepository reservationRepo;
    private final RoomRepository roomRepo;

    public ReportService(ReservationRepository reservationRepo, RoomRepository roomRepo) {
        this.reservationRepo = reservationRepo;
        this.roomRepo = roomRepo;
    }

    public String generateOccupancyReport(LocalDate date) {
        StringBuilder csv = new StringBuilder();

        csv.append(POUSADA_NAME).append(",occupancy,").append(formatDate(date)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        Map<Integer, Room> allRooms = roomRepo.findAll();
        List<Integer> roomNumbers = new ArrayList<>(allRooms.keySet());
        Collections.sort(roomNumbers);

        int occupiedCount = 0;
        for (int roomNumber : roomNumbers) {
            Room room = allRooms.get(roomNumber);
            boolean occupied = false;

            for (Reservation res : reservationRepo.findAll().values()) {
                if (res.getRoomNumber() == roomNumber && res.getStatus() != ReservationStatus.CANCELLED) {
                    if (!date.isBefore(res.getCheckIn()) && date.isBefore(res.getCheckOut())) {
                        csv.append(roomNumber).append(",")
                           .append(room.getType()).append(",")
                           .append(res.getId()).append(",")
                           .append(formatDate(res.getCheckIn())).append(",")
                           .append(formatDate(res.getCheckOut())).append("\n");
                        occupied = true;
                        break;
                    }
                }
            }

            if (occupied) {
                occupiedCount++;
            }
        }

        int totalRooms = roomRepo.findAll().size();
        csv.append("total,").append(occupiedCount).append("/").append(totalRooms).append("\n");

        return csv.toString();
    }

    public String generateRevenueReport(YearMonth yearMonth) {
        StringBuilder csv = new StringBuilder();

        csv.append(POUSADA_NAME).append(",revenue,").append(yearMonth).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        List<Reservation> reservations = reservationRepo.findAll().values().stream()
            .filter(res -> YearMonth.from(res.getCheckIn()).equals(yearMonth))
            .filter(res -> (res.getStatus() == ReservationStatus.CONFIRMED ||
                           res.getStatus() == ReservationStatus.CHECKED_IN ||
                           res.getStatus() == ReservationStatus.CHECKED_OUT) ||
                           (res.getStatus() == ReservationStatus.CANCELLED && res.getRatePlan() == RatePlan.NON_REFUNDABLE))
            .sorted((r1, r2) -> {
                int dateCompare = r1.getCheckIn().compareTo(r2.getCheckIn());
                if (dateCompare != 0) return dateCompare;
                return Long.compare(r1.getId(), r2.getId());
            })
            .collect(Collectors.toList());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Reservation res : reservations) {
            BigDecimal amount = res.getAmountPaid().subtract(
                res.getRefund() != null ? res.getRefund() : BigDecimal.ZERO
            ).setScale(2, RoundingMode.HALF_EVEN);

            csv.append(res.getId()).append(",")
               .append(res.getRoomNumber()).append(",")
               .append(formatDate(res.getCheckIn())).append(",")
               .append(formatDate(res.getCheckOut())).append(",")
               .append(res.getStatus()).append(",")
               .append(formatMoney(amount)).append("\n");

            totalAmount = totalAmount.add(amount);
        }

        csv.append("total,").append(formatMoney(totalAmount)).append("\n");

        return csv.toString();
    }

    public String generateTourismTaxReport(YearMonth yearMonth) {
        StringBuilder csv = new StringBuilder();

        csv.append(POUSADA_NAME).append(",tourism-tax,").append(yearMonth).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        List<Reservation> reservations = reservationRepo.findAll().values().stream()
            .filter(res -> YearMonth.from(res.getCheckIn()).equals(yearMonth))
            .filter(res -> res.getStatus() == ReservationStatus.CONFIRMED ||
                           res.getStatus() == ReservationStatus.CHECKED_IN ||
                           res.getStatus() == ReservationStatus.CHECKED_OUT)
            .sorted((r1, r2) -> {
                int dateCompare = r1.getCheckIn().compareTo(r2.getCheckIn());
                if (dateCompare != 0) return dateCompare;
                return Long.compare(r1.getId(), r2.getId());
            })
            .collect(Collectors.toList());

        BigDecimal totalTax = BigDecimal.ZERO;
        for (Reservation res : reservations) {
            BigDecimal tax = TOURISM_TAX_PER_GUEST_NIGHT
                .multiply(new BigDecimal(res.getGuests()))
                .multiply(new BigDecimal(res.getNights()))
                .setScale(2, RoundingMode.HALF_EVEN);

            csv.append(res.getId()).append(",")
               .append(res.getRoomNumber()).append(",")
               .append(formatDate(res.getCheckIn())).append(",")
               .append(formatDate(res.getCheckOut())).append(",")
               .append(res.getGuests()).append(",")
               .append(res.getNights()).append(",")
               .append(formatMoney(tax)).append("\n");

            totalTax = totalTax.add(tax);
        }

        csv.append("total,").append(formatMoney(totalTax)).append("\n");

        return csv.toString();
    }

    public String generateRatePlansReport(YearMonth yearMonth) {
        StringBuilder csv = new StringBuilder();

        csv.append(POUSADA_NAME).append(",rate-plans,").append(yearMonth).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        List<Reservation> reservations = reservationRepo.findAll().values().stream()
            .filter(res -> YearMonth.from(res.getCheckIn()).equals(yearMonth))
            .filter(res -> (res.getStatus() == ReservationStatus.CONFIRMED ||
                           res.getStatus() == ReservationStatus.CHECKED_IN ||
                           res.getStatus() == ReservationStatus.CHECKED_OUT) ||
                           (res.getStatus() == ReservationStatus.CANCELLED && res.getRatePlan() == RatePlan.NON_REFUNDABLE))
            .collect(Collectors.toList());

        Map<RatePlan, List<Reservation>> byRatePlan = new HashMap<>();
        for (Reservation res : reservations) {
            byRatePlan.computeIfAbsent(res.getRatePlan(), k -> new ArrayList<>()).add(res);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalReservations = 0;

        for (RatePlan ratePlan : RatePlan.values()) {
            List<Reservation> planReservations = byRatePlan.getOrDefault(ratePlan, new ArrayList<>());
            BigDecimal amount = BigDecimal.ZERO;

            for (Reservation res : planReservations) {
                BigDecimal resAmount = res.getAmountPaid().subtract(
                    res.getRefund() != null ? res.getRefund() : BigDecimal.ZERO
                ).setScale(2, RoundingMode.HALF_EVEN);
                amount = amount.add(resAmount);
            }

            int count = planReservations.size();
            csv.append(ratePlan).append(",")
               .append(count).append(",")
               .append(formatMoney(amount)).append("\n");

            totalAmount = totalAmount.add(amount);
            totalReservations += count;
        }

        csv.append("total,").append(totalReservations).append(",").append(formatMoney(totalAmount)).append("\n");

        return csv.toString();
    }

    private String formatDate(LocalDate date) {
        return date.toString();
    }

    private String formatMoney(BigDecimal value) {
        return String.format("%.2f", value);
    }
}
