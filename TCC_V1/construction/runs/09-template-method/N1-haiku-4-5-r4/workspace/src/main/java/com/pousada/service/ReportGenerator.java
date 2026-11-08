package com.pousada.service;

import com.pousada.db.Database;
import com.pousada.model.Reservation;
import com.pousada.model.RatePlan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportGenerator {
    private final Database database;

    public ReportGenerator(Database database) {
        this.database = database;
    }

    public String generateOccupancyReport(LocalDate date) throws Exception {
        List<Reservation> reservations = database.getReservationsForOccupancy(date);

        StringBuilder sb = new StringBuilder();
        sb.append("Pousada Maré Alta,occupancy,").append(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        sb.append("room,type,reservation,check_in,check_out\n");

        for (Reservation res : reservations) {
            String roomType = database.getRoomByNumber(res.getRoomNumber()).getType().name();
            sb.append(res.getRoomNumber()).append(",")
              .append(roomType).append(",")
              .append(res.getId()).append(",")
              .append(res.getCheckIn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append(",")
              .append(res.getCheckOut().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        }

        sb.append("total,").append(reservations.size()).append("/5\n");

        return sb.toString();
    }

    public String generateRevenueReport(YearMonth month) throws Exception {
        List<Reservation> reservations = database.getReservationsForMonth(month.getYear(), month.getMonthValue());

        StringBuilder sb = new StringBuilder();
        sb.append("Pousada Maré Alta,revenue,").append(month.format(DateTimeFormatter.ofPattern("yyyy-MM"))).append("\n");
        sb.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amountForPousada = res.getAmountPaid();
            if (res.getRefund() != null && res.getRefund().compareTo(BigDecimal.ZERO) > 0) {
                amountForPousada = amountForPousada.subtract(res.getRefund());
            }

            sb.append(res.getId()).append(",")
              .append(res.getRoomNumber()).append(",")
              .append(res.getCheckIn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append(",")
              .append(res.getCheckOut().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append(",")
              .append(res.getStatus().name()).append(",")
              .append(formatDecimal(amountForPousada)).append("\n");

            totalAmount = totalAmount.add(amountForPousada);
        }

        sb.append("total,").append(formatDecimal(totalAmount)).append("\n");

        return sb.toString();
    }

    public String generateTourismTaxReport(YearMonth month) throws Exception {
        List<Reservation> reservations = database.getReservationsForMonth(month.getYear(), month.getMonthValue());

        StringBuilder sb = new StringBuilder();
        sb.append("Pousada Maré Alta,tourism-tax,").append(month.format(DateTimeFormatter.ofPattern("yyyy-MM"))).append("\n");
        sb.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal tourismTaxRate = new BigDecimal("4.50");
        BigDecimal totalTax = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal tax = calculateTourismTax(res.getGuests(), res.getNights(), tourismTaxRate);

            sb.append(res.getId()).append(",")
              .append(res.getRoomNumber()).append(",")
              .append(res.getCheckIn().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append(",")
              .append(res.getCheckOut().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append(",")
              .append(res.getGuests()).append(",")
              .append(res.getNights()).append(",")
              .append(formatDecimal(tax)).append("\n");

            totalTax = totalTax.add(tax);
        }

        sb.append("total,").append(formatDecimal(totalTax)).append("\n");

        return sb.toString();
    }

    public String generateRatePlanReport(YearMonth month) throws Exception {
        List<Reservation> reservations = database.getReservationsForMonth(month.getYear(), month.getMonthValue());

        StringBuilder sb = new StringBuilder();
        sb.append("Pousada Maré Alta,rate-plans,").append(month.format(DateTimeFormatter.ofPattern("yyyy-MM"))).append("\n");
        sb.append("rate_plan,reservations,amount\n");

        int standardCount = 0;
        int nonRefundableCount = 0;
        BigDecimal standardAmount = BigDecimal.ZERO;
        BigDecimal nonRefundableAmount = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amountForPousada = res.getAmountPaid();
            if (res.getRefund() != null && res.getRefund().compareTo(BigDecimal.ZERO) > 0) {
                amountForPousada = amountForPousada.subtract(res.getRefund());
            }

            if (res.getRatePlan() == RatePlan.STANDARD) {
                standardCount++;
                standardAmount = standardAmount.add(amountForPousada);
            } else {
                nonRefundableCount++;
                nonRefundableAmount = nonRefundableAmount.add(amountForPousada);
            }
        }

        sb.append("STANDARD,").append(standardCount).append(",").append(formatDecimal(standardAmount)).append("\n");
        sb.append("NON_REFUNDABLE,").append(nonRefundableCount).append(",").append(formatDecimal(nonRefundableAmount)).append("\n");

        BigDecimal totalAmount = standardAmount.add(nonRefundableAmount);
        int totalCount = standardCount + nonRefundableCount;
        sb.append("total,").append(totalCount).append(",").append(formatDecimal(totalAmount)).append("\n");

        return sb.toString();
    }

    private BigDecimal calculateTourismTax(int guests, int nights, BigDecimal rate) {
        BigDecimal taxValue = new BigDecimal(guests).multiply(new BigDecimal(nights)).multiply(rate);
        return taxValue.setScale(2, java.math.RoundingMode.HALF_EVEN);
    }

    private String formatDecimal(BigDecimal value) {
        return String.format("%.2f", value);
    }
}
