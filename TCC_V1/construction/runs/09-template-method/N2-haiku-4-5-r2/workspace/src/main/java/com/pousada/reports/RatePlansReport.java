package com.pousada.reports;

import com.pousada.domain.RatePlan;
import com.pousada.domain.ReservationStatus;
import com.pousada.domain.Rounding;
import com.pousada.infrastructure.ReservationRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RatePlansReport extends Report {
    private final ReservationRepository reservationRepository = new ReservationRepository();

    @Override
    protected String getReportName() {
        return "rate-plans";
    }

    @Override
    protected String getReportPeriod(LocalDate date) {
        return YearMonth.from(date).toString();
    }

    @Override
    protected String generateHeader() {
        return "rate_plan,reservations,amount";
    }

    @Override
    protected List<String> generateRows(LocalDate date) throws Exception {
        List<String> rows = new ArrayList<>();
        LocalDate startOfMonth = date.withDayOfMonth(1);

        var reservations = reservationRepository.findByMonthStarting(startOfMonth);

        for (RatePlan ratePlan : RatePlan.values()) {
            int count = 0;
            double amount = 0;

            for (var reservation : reservations) {
                if (isPaidReservation(reservation.getStatus()) && reservation.getRatePlan() == ratePlan) {
                    count++;
                    amount += calculateAmount(reservation);
                }
            }

            rows.add(String.format("%s,%d,%.2f",
                ratePlan.getDisplayName(),
                count,
                Rounding.round(amount)
            ));
        }

        return rows;
    }

    @Override
    protected String generateTotal(LocalDate date) throws Exception {
        LocalDate startOfMonth = date.withDayOfMonth(1);
        var reservations = reservationRepository.findByMonthStarting(startOfMonth);

        int totalCount = 0;
        double totalAmount = 0;

        for (var reservation : reservations) {
            if (isPaidReservation(reservation.getStatus())) {
                totalCount++;
                totalAmount += calculateAmount(reservation);
            }
        }

        return String.format("total,%d,%.2f", totalCount, Rounding.round(totalAmount));
    }

    private boolean isPaidReservation(ReservationStatus status) {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
    }

    private double calculateAmount(com.pousada.domain.Reservation reservation) {
        double paid = reservation.getAmountPaid();
        Double refund = reservation.getRefund();
        if (refund != null) {
            return Rounding.round(paid - refund);
        }
        return paid;
    }
}
