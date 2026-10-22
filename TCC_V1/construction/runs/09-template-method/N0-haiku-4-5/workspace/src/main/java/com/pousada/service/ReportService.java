package com.pousada.service;

import com.pousada.model.Reservation;
import com.pousada.model.ReservationStatus;
import com.pousada.model.RatePlan;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

public class ReportService {

    public static String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = ReservationRepository.findOccupancyByDate(date);

        StringBuilder csv = new StringBuilder();
        csv.append(String.format("Pousada Maré Alta,occupancy,%s\n", formatDate(date)));
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Reservation r : reservations) {
            var room = RoomRepository.findByNumber(r.getRoomNumber()).get();
            csv.append(String.format("%d,%s,%d,%s,%s\n",
                r.getRoomNumber(),
                room.getType(),
                r.getId(),
                formatDateISO(r.getCheckIn()),
                formatDateISO(r.getCheckOut())));
        }

        int occupied = reservations.size();
        csv.append(String.format("total,%d/5\n", occupied));

        return csv.toString();
    }

    public static String generateRevenueReport(YearMonth yearMonth) {
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();

        List<Reservation> allReservations = ReservationRepository.findByMonthStart(year, month);
        List<Reservation> paidReservations = allReservations.stream()
            .filter(r -> r.getAmountPaid().compareTo(BigDecimal.ZERO) > 0)
            .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append(String.format("Pousada Maré Alta,revenue,%s\n", yearMonth));
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;
        for (Reservation r : paidReservations) {
            BigDecimal amount = r.getAmountPaid().subtract(r.getRefund() == null ? BigDecimal.ZERO : r.getRefund());
            csv.append(String.format("%d,%d,%s,%s,%s,%.2f\n",
                r.getId(),
                r.getRoomNumber(),
                formatDateISO(r.getCheckIn()),
                formatDateISO(r.getCheckOut()),
                r.getStatus(),
                amount));
            total = total.add(amount);
        }

        csv.append(String.format("total,%.2f\n", total));

        return csv.toString();
    }

    public static String generateTourismTaxReport(YearMonth yearMonth) {
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();

        List<Reservation> allReservations = ReservationRepository.findByMonthStart(year, month);
        List<Reservation> paidReservations = allReservations.stream()
            .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED ||
                        r.getStatus() == ReservationStatus.CHECKED_IN ||
                        r.getStatus() == ReservationStatus.CHECKED_OUT)
            .collect(Collectors.toList());

        StringBuilder csv = new StringBuilder();
        csv.append(String.format("Pousada Maré Alta,tourism-tax,%s\n", yearMonth));
        csv.append("reservation,room,check_in,check_out,guests,nights,tax\n");

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal taxPerGuestPerNight = new BigDecimal("4.50");

        for (Reservation r : paidReservations) {
            BigDecimal tax = taxPerGuestPerNight
                .multiply(BigDecimal.valueOf(r.getGuests()))
                .multiply(BigDecimal.valueOf(r.getNights()));
            tax = PricingCalculator.roundHalfToEven(tax);

            csv.append(String.format("%d,%d,%s,%s,%d,%d,%.2f\n",
                r.getId(),
                r.getRoomNumber(),
                formatDateISO(r.getCheckIn()),
                formatDateISO(r.getCheckOut()),
                r.getGuests(),
                r.getNights(),
                tax));
            total = total.add(tax);
        }

        csv.append(String.format("total,%.2f\n", total));

        return csv.toString();
    }

    public static String generateRatePlansReport(YearMonth yearMonth) {
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();

        List<Reservation> allReservations = ReservationRepository.findByMonthStart(year, month);
        List<Reservation> paidReservations = allReservations.stream()
            .filter(r -> r.getAmountPaid().compareTo(BigDecimal.ZERO) > 0)
            .collect(Collectors.toList());

        BigDecimal standardAmount = BigDecimal.ZERO;
        int standardCount = 0;
        BigDecimal nonRefundableAmount = BigDecimal.ZERO;
        int nonRefundableCount = 0;

        for (Reservation r : paidReservations) {
            BigDecimal amount = r.getAmountPaid().subtract(r.getRefund() == null ? BigDecimal.ZERO : r.getRefund());
            if (r.getRatePlan() == RatePlan.STANDARD) {
                standardAmount = standardAmount.add(amount);
                standardCount++;
            } else {
                nonRefundableAmount = nonRefundableAmount.add(amount);
                nonRefundableCount++;
            }
        }

        StringBuilder csv = new StringBuilder();
        csv.append(String.format("Pousada Maré Alta,rate-plans,%s\n", yearMonth));
        csv.append("rate_plan,reservations,amount\n");

        csv.append(String.format("STANDARD,%d,%.2f\n", standardCount, standardAmount));
        csv.append(String.format("NON_REFUNDABLE,%d,%.2f\n", nonRefundableCount, nonRefundableAmount));

        BigDecimal totalAmount = standardAmount.add(nonRefundableAmount);
        int totalCount = standardCount + nonRefundableCount;
        csv.append(String.format("total,%d,%.2f\n", totalCount, totalAmount));

        return csv.toString();
    }

    private static String formatDate(LocalDate date) {
        return date.toString();
    }

    private static String formatDateISO(LocalDate date) {
        return date.toString();
    }
}
