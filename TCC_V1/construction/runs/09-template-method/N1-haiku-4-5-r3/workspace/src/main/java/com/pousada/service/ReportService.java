package com.pousada.service;

import com.pousada.model.Reservation;
import com.pousada.model.ReservationStatus;
import com.pousada.model.RatePlan;
import com.pousada.model.Room;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.util.PriceUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportService {
    private static final String LODGE_NAME = "Pousada Maré Alta";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final BigDecimal TOURISM_TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    public static String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = ReservationRepository.findByDateAndStatus(date, ReservationStatus.CONFIRMED);
        reservations.addAll(ReservationRepository.findByDateAndStatus(date, ReservationStatus.CHECKED_IN));
        reservations.addAll(ReservationRepository.findByDateAndStatus(date, ReservationStatus.CHECKED_OUT));

        Map<Integer, Reservation> occupancyByRoom = new TreeMap<>();
        for (Reservation r : reservations) {
            occupancyByRoom.put(r.getRoomNumber(), r);
        }

        List<Room> allRooms = RoomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append(LODGE_NAME).append(",occupancy,").append(date.format(DATE_FORMATTER)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Room room : allRooms) {
            if (occupancyByRoom.containsKey(room.getNumber())) {
                Reservation r = occupancyByRoom.get(room.getNumber());
                csv.append(r.getRoomNumber()).append(",")
                   .append(room.getType().getName()).append(",")
                   .append(r.getId()).append(",")
                   .append(r.getCheckIn().format(DATE_FORMATTER)).append(",")
                   .append(r.getCheckOut().format(DATE_FORMATTER)).append("\n");
            }
        }

        csv.append("total,").append(occupancyByRoom.size()).append("/").append(allRooms.size()).append("\n");

        return csv.toString();
    }

    public static String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = ReservationRepository.findByMonthForRevenue(month.getYear(), month.getMonthValue());

        StringBuilder csv = new StringBuilder();
        csv.append(LODGE_NAME).append(",revenue,").append(month).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal netAmount = r.getAmountPaid();
            if (r.getRefund() != null) {
                netAmount = PriceUtil.subtract(netAmount, r.getRefund());
            }

            csv.append(r.getId()).append(",")
               .append(r.getRoomNumber()).append(",")
               .append(r.getCheckIn().format(DATE_FORMATTER)).append(",")
               .append(r.getCheckOut().format(DATE_FORMATTER)).append(",")
               .append(r.getStatus().name()).append(",")
               .append(formatCurrencyForCsv(netAmount)).append("\n");

            total = PriceUtil.add(total, netAmount);
        }

        csv.append("total,,,,").append(formatCurrencyForCsv(total)).append("\n");

        return csv.toString();
    }

    public static String generateTourismTaxReport(YearMonth month) {
        List<Reservation> reservations = ReservationRepository.findByMonthForRevenue(month.getYear(), month.getMonthValue());

        StringBuilder csv = new StringBuilder();
        csv.append(LODGE_NAME).append(",tourism-tax,").append(month).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation r : reservations) {
            BigDecimal tax = PriceUtil.multiply(
                TOURISM_TAX_PER_GUEST_PER_NIGHT,
                new BigDecimal(r.getGuests() * r.getNights())
            );

            csv.append(r.getId()).append(",")
               .append(r.getRoomNumber()).append(",")
               .append(r.getCheckIn().format(DATE_FORMATTER)).append(",")
               .append(r.getCheckOut().format(DATE_FORMATTER)).append(",")
               .append(r.getGuests()).append(",")
               .append(r.getNights()).append(",")
               .append(formatCurrencyForCsv(tax)).append("\n");

            total = PriceUtil.add(total, tax);
        }

        csv.append("total,,,,,").append(formatCurrencyForCsv(total)).append("\n");

        return csv.toString();
    }

    public static String generateRatePlansReport(YearMonth month) {
        List<Reservation> reservations = ReservationRepository.findByMonthForRevenue(month.getYear(), month.getMonthValue());

        Map<RatePlan, Integer> reservationCounts = new TreeMap<>(Comparator.comparing(RatePlan::getName));
        Map<RatePlan, BigDecimal> amounts = new TreeMap<>(Comparator.comparing(RatePlan::getName));

        for (RatePlan plan : RatePlan.values()) {
            reservationCounts.put(plan, 0);
            amounts.put(plan, BigDecimal.ZERO);
        }

        for (Reservation r : reservations) {
            RatePlan plan = r.getRatePlan();
            reservationCounts.put(plan, reservationCounts.get(plan) + 1);

            BigDecimal netAmount = r.getAmountPaid();
            if (r.getRefund() != null) {
                netAmount = PriceUtil.subtract(netAmount, r.getRefund());
            }
            amounts.put(plan, PriceUtil.add(amounts.get(plan), netAmount));
        }

        StringBuilder csv = new StringBuilder();
        csv.append(LODGE_NAME).append(",rate-plans,").append(month).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        int totalReservations = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (RatePlan plan : RatePlan.values()) {
            int count = reservationCounts.get(plan);
            BigDecimal amount = amounts.get(plan);

            csv.append(plan.getName()).append(",")
               .append(count).append(",")
               .append(formatCurrencyForCsv(amount)).append("\n");

            totalReservations += count;
            totalAmount = PriceUtil.add(totalAmount, amount);
        }

        csv.append("total,").append(totalReservations).append(",").append(formatCurrencyForCsv(totalAmount)).append("\n");

        return csv.toString();
    }

    private static String formatCurrencyForCsv(BigDecimal value) {
        return String.format("%.2f", value);
    }
}
