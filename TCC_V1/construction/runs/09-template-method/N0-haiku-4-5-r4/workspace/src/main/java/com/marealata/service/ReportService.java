package com.marealata.service;

import com.marealata.dao.*;
import com.marealata.model.*;
import com.marealata.util.DocumentFormatter;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

public class ReportService {
    private static final String POUSADA_NAME = "Pousada Maré Alta";
    private static final BigDecimal TOURISM_TAX_PER_GUEST_PER_NIGHT = new BigDecimal("4.50");

    public static String generateOccupancyReport(LocalDate date) throws SQLException {
        List<Reservation> reservations = ReservationDao.findForOccupancyReport(date);

        StringBuilder csv = new StringBuilder();
        csv.append(POUSADA_NAME).append(",occupancy,").append(DocumentFormatter.formatDateCsv(date)).append("\n");
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation res : reservations) {
            Room room = RoomDao.findByNumber(res.roomNumber);
            csv.append(res.roomNumber).append(",")
               .append(room.type).append(",")
               .append(res.id).append(",")
               .append(DocumentFormatter.formatDateCsv(res.checkIn)).append(",")
               .append(DocumentFormatter.formatDateCsv(res.checkOut)).append("\n");
        }

        int occupied = reservations.size();
        csv.append("total,").append(occupied).append("/5\n");

        return csv.toString();
    }

    public static String generateRevenueReport(YearMonth month) throws SQLException {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth().plusDays(1);

        List<Reservation> reservations = ReservationDao.findForRevenueReport(monthStart, monthEnd);

        StringBuilder csv = new StringBuilder();
        csv.append(POUSADA_NAME).append(",revenue,").append(month).append("\n");
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amountPaid = res.amountPaid != null ? res.amountPaid : BigDecimal.ZERO;
            BigDecimal refund = res.refund != null ? res.refund : BigDecimal.ZERO;
            BigDecimal amount = amountPaid.subtract(refund);

            csv.append(res.id).append(",")
               .append(res.roomNumber).append(",")
               .append(DocumentFormatter.formatDateCsv(res.checkIn)).append(",")
               .append(DocumentFormatter.formatDateCsv(res.checkOut)).append(",")
               .append(res.status).append(",")
               .append(DocumentFormatter.formatCurrencyCsv(amount)).append("\n");

            total = total.add(amount);
        }

        csv.append("total,,,,").append(DocumentFormatter.formatCurrencyCsv(total)).append("\n");

        return csv.toString();
    }

    public static String generateTourismTaxReport(YearMonth month) throws SQLException {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth().plusDays(1);

        List<Reservation> reservations = ReservationDao.findForRevenueReport(monthStart, monthEnd);

        StringBuilder csv = new StringBuilder();
        csv.append(POUSADA_NAME).append(",tourism-tax,").append(month).append("\n");
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal totalTax = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal tax = TOURISM_TAX_PER_GUEST_PER_NIGHT
                .multiply(BigDecimal.valueOf(res.guests))
                .multiply(BigDecimal.valueOf(res.nights));
            tax = tax.setScale(2, java.math.RoundingMode.HALF_EVEN);

            csv.append(res.id).append(",")
               .append(res.roomNumber).append(",")
               .append(DocumentFormatter.formatDateCsv(res.checkIn)).append(",")
               .append(DocumentFormatter.formatDateCsv(res.checkOut)).append(",")
               .append(res.guests).append(",")
               .append(res.nights).append(",")
               .append(DocumentFormatter.formatCurrencyCsv(tax)).append("\n");

            totalTax = totalTax.add(tax);
        }

        csv.append("total,,,,,,").append(DocumentFormatter.formatCurrencyCsv(totalTax)).append("\n");

        return csv.toString();
    }

    public static String generateRatePlansReport(YearMonth month) throws SQLException {
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth().plusDays(1);

        List<Reservation> reservations = ReservationDao.findForRevenueReport(monthStart, monthEnd);

        int standardCount = 0;
        BigDecimal standardAmount = BigDecimal.ZERO;
        int nonRefundableCount = 0;
        BigDecimal nonRefundableAmount = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amountPaid = res.amountPaid != null ? res.amountPaid : BigDecimal.ZERO;
            BigDecimal refund = res.refund != null ? res.refund : BigDecimal.ZERO;
            BigDecimal amount = amountPaid.subtract(refund);

            if (res.ratePlan == RatePlan.STANDARD) {
                standardCount++;
                standardAmount = standardAmount.add(amount);
            } else {
                nonRefundableCount++;
                nonRefundableAmount = nonRefundableAmount.add(amount);
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append(POUSADA_NAME).append(",rate-plans,").append(month).append("\n");
        csv.append("rate_plan,reservations,amount\n");

        csv.append("STANDARD,").append(standardCount).append(",")
           .append(DocumentFormatter.formatCurrencyCsv(standardAmount)).append("\n");

        csv.append("NON_REFUNDABLE,").append(nonRefundableCount).append(",")
           .append(DocumentFormatter.formatCurrencyCsv(nonRefundableAmount)).append("\n");

        int totalCount = standardCount + nonRefundableCount;
        BigDecimal totalAmount = standardAmount.add(nonRefundableAmount);
        csv.append("total,").append(totalCount).append(",")
           .append(DocumentFormatter.formatCurrencyCsv(totalAmount)).append("\n");

        return csv.toString();
    }
}
