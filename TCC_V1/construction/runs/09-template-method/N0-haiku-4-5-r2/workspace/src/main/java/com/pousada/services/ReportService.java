package com.pousada.services;

import com.pousada.models.Reservation;
import com.pousada.models.Room;
import com.pousada.repositories.ReservationRepository;
import com.pousada.repositories.RoomRepository;
import com.pousada.utils.DateUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ReportService {
    private static final DecimalFormat MONEY_FORMAT = createMoneyFormat();
    private static final BigDecimal TOURISM_TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    private static DecimalFormat createMoneyFormat() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat format = new DecimalFormat("0.00", symbols);
        return format;
    }

    private static String formatMoney(BigDecimal value) {
        return MONEY_FORMAT.format(value);
    }

    public static String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = ReservationRepository.findForOccupancy(date);

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,occupancy,").append(DateUtils.formatIsoDate(date)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation res : reservations) {
            Room room = RoomRepository.findByNumber(res.getRoomNumber());
            csv.append(res.getRoomNumber()).append(",")
                .append(room.getType()).append(",")
                .append(res.getId()).append(",")
                .append(DateUtils.formatIsoDate(res.getCheckIn())).append(",")
                .append(DateUtils.formatIsoDate(res.getCheckOut())).append("\n");
        }

        int totalRooms = RoomRepository.findAll().size();
        csv.append("total,").append(reservations.size()).append("/").append(totalRooms);

        return csv.toString();
    }

    public static String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = ReservationRepository.findForRevenue(month);

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,revenue,").append(month).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amount = res.getAmountPaid();
            if (res.getRefund() != null) {
                amount = amount.subtract(res.getRefund());
            }

            csv.append(res.getId()).append(",")
                .append(res.getRoomNumber()).append(",")
                .append(DateUtils.formatIsoDate(res.getCheckIn())).append(",")
                .append(DateUtils.formatIsoDate(res.getCheckOut())).append(",")
                .append(res.getStatus()).append(",")
                .append(formatMoney(amount)).append("\n");

            totalAmount = totalAmount.add(amount);
        }

        csv.append("total,,,,").append(formatMoney(totalAmount));

        return csv.toString();
    }

    public static String generateTourismTaxReport(YearMonth month) {
        List<Reservation> reservations = ReservationRepository.findForRevenue(month);

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,tourism-tax,").append(month).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal totalTax = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            if (isPaidReservation(res)) {
                BigDecimal tax = TOURISM_TAX_PER_GUEST_PER_NIGHT
                    .multiply(new BigDecimal(res.getGuests()))
                    .multiply(new BigDecimal(res.getNights()));

                csv.append(res.getId()).append(",")
                    .append(res.getRoomNumber()).append(",")
                    .append(DateUtils.formatIsoDate(res.getCheckIn())).append(",")
                    .append(DateUtils.formatIsoDate(res.getCheckOut())).append(",")
                    .append(res.getGuests()).append(",")
                    .append(res.getNights()).append(",")
                    .append(formatMoney(tax)).append("\n");

                totalTax = totalTax.add(tax);
            }
        }

        csv.append("total,,,,,").append(formatMoney(totalTax));

        return csv.toString();
    }

    public static String generateRatePlanReport(YearMonth month) {
        List<Reservation> reservations = ReservationRepository.findForRevenue(month);

        Map<String, BigDecimal> planAmounts = new LinkedHashMap<>();
        Map<String, Integer> planCounts = new LinkedHashMap<>();

        planAmounts.put("STANDARD", BigDecimal.ZERO);
        planAmounts.put("NON_REFUNDABLE", BigDecimal.ZERO);
        planCounts.put("STANDARD", 0);
        planCounts.put("NON_REFUNDABLE", 0);

        for (Reservation res : reservations) {
            BigDecimal amount = res.getAmountPaid();
            if (res.getRefund() != null) {
                amount = amount.subtract(res.getRefund());
            }

            String plan = res.getRatePlan();
            planAmounts.put(plan, planAmounts.getOrDefault(plan, BigDecimal.ZERO).add(amount));
            planCounts.put(plan, planCounts.getOrDefault(plan, 0) + 1);
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Pousada Maré Alta,rate-plans,").append(month).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalCount = 0;

        for (String plan : Arrays.asList("STANDARD", "NON_REFUNDABLE")) {
            BigDecimal amount = planAmounts.get(plan);
            int count = planCounts.get(plan);

            csv.append(plan).append(",")
                .append(count).append(",")
                .append(formatMoney(amount)).append("\n");

            totalAmount = totalAmount.add(amount);
            totalCount = totalCount + count;
        }

        csv.append("total,").append(totalCount).append(",").append(formatMoney(totalAmount));

        return csv.toString();
    }

    private static boolean isPaidReservation(Reservation res) {
        return "CONFIRMED".equals(res.getStatus()) ||
               "CHECKED_IN".equals(res.getStatus()) ||
               "CHECKED_OUT".equals(res.getStatus());
    }
}
