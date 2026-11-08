package com.pousadamarealta.service;

import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.util.CurrencyFormatter;
import com.pousadamarealta.util.DateFormatter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportService {
    private final ReservationRepository reservationRepository = new ReservationRepository();
    private final RoomRepository roomRepository = new RoomRepository();

    public String generateOccupancyReport(LocalDate date) throws Exception {
        List<Reservation> reservations = reservationRepository.findByDate(date);
        List<Room> allRooms = roomRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,occupancy,").append(DateFormatter.formatISO(date)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;
        for (Room room : allRooms) {
            boolean found = false;
            for (Reservation res : reservations) {
                if (res.roomNumber == room.number) {
                    csv.append(room.number).append(",")
                        .append(room.type).append(",")
                        .append(res.id).append(",")
                        .append(DateFormatter.formatISO(res.checkIn)).append(",")
                        .append(DateFormatter.formatISO(res.checkOut)).append("\n");
                    occupiedCount++;
                    found = true;
                    break;
                }
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(allRooms.size()).append("\n");
        return csv.toString();
    }

    public String generateRevenueReport(YearMonth month) throws Exception {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth().plusDays(1);

        List<Reservation> reservations = reservationRepository.findByMonth(monthStart, monthEnd);

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,revenue,").append(month).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            if ("CANCELLED".equals(res.status)) {
                // Only count cancelled if they have refund info (i.e., were confirmed)
                if (res.amountPaid.compareTo(BigDecimal.ZERO) == 0) continue;
            }

            if (!"PENDING".equals(res.status)) {
                BigDecimal amount = res.amountPaid;
                if (res.refund != null) {
                    amount = amount.subtract(res.refund);
                }

                csv.append(res.id).append(",")
                    .append(res.roomNumber).append(",")
                    .append(DateFormatter.formatISO(res.checkIn)).append(",")
                    .append(DateFormatter.formatISO(res.checkOut)).append(",")
                    .append(res.status).append(",")
                    .append(CurrencyFormatter.formatForCsv(amount)).append("\n");

                total = total.add(amount);
            }
        }

        csv.append("total,,,,").append(CurrencyFormatter.formatForCsv(total)).append("\n");
        return csv.toString();
    }

    public String generateTourismTaxReport(YearMonth month) throws Exception {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth().plusDays(1);

        List<Reservation> reservations = reservationRepository.findByMonth(monthStart, monthEnd);
        BigDecimal TAX_PER_GUEST_NIGHT = new BigDecimal("4.50");

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,tourism-tax,").append(month).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            // Only include CONFIRMED, CHECKED_IN, CHECKED_OUT
            if ("CONFIRMED".equals(res.status) || "CHECKED_IN".equals(res.status) || "CHECKED_OUT".equals(res.status)) {
                BigDecimal tax = new BigDecimal(res.guests).multiply(new BigDecimal(res.nights))
                    .multiply(TAX_PER_GUEST_NIGHT);

                csv.append(res.id).append(",")
                    .append(res.roomNumber).append(",")
                    .append(DateFormatter.formatISO(res.checkIn)).append(",")
                    .append(DateFormatter.formatISO(res.checkOut)).append(",")
                    .append(res.guests).append(",")
                    .append(res.nights).append(",")
                    .append(CurrencyFormatter.formatForCsv(tax)).append("\n");

                total = total.add(tax);
            }
        }

        csv.append("total,,,,,").append(CurrencyFormatter.formatForCsv(total)).append("\n");
        return csv.toString();
    }

    public String generateRatePlansReport(YearMonth month) throws Exception {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth().plusDays(1);

        List<Reservation> reservations = reservationRepository.findByMonth(monthStart, monthEnd);

        Map<String, Integer> countByPlan = new HashMap<>();
        Map<String, BigDecimal> amountByPlan = new HashMap<>();

        countByPlan.put("STANDARD", 0);
        countByPlan.put("NON_REFUNDABLE", 0);
        amountByPlan.put("STANDARD", BigDecimal.ZERO);
        amountByPlan.put("NON_REFUNDABLE", BigDecimal.ZERO);

        for (Reservation res : reservations) {
            if ("CANCELLED".equals(res.status)) {
                if (res.amountPaid.compareTo(BigDecimal.ZERO) == 0) continue;
            }

            if (!"PENDING".equals(res.status)) {
                String plan = res.ratePlan;
                countByPlan.put(plan, countByPlan.get(plan) + 1);

                BigDecimal amount = res.amountPaid;
                if (res.refund != null) {
                    amount = amount.subtract(res.refund);
                }
                amountByPlan.put(plan, amountByPlan.get(plan).add(amount));
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalCount = 0;

        for (String plan : new String[]{"STANDARD", "NON_REFUNDABLE"}) {
            int count = countByPlan.get(plan);
            BigDecimal amount = amountByPlan.get(plan);

            csv.append(plan).append(",")
                .append(count).append(",")
                .append(CurrencyFormatter.formatForCsv(amount)).append("\n");

            totalAmount = totalAmount.add(amount);
            totalCount += count;
        }

        csv.append("total,").append(totalCount).append(",")
            .append(CurrencyFormatter.formatForCsv(totalAmount)).append("\n");

        return csv.toString();
    }
}
