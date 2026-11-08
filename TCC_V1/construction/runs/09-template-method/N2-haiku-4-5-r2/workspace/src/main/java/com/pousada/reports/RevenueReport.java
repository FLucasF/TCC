package com.pousada.reports;

import com.pousada.domain.ReservationStatus;
import com.pousada.domain.Rounding;
import com.pousada.infrastructure.ReservationRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class RevenueReport extends Report {
    private final ReservationRepository reservationRepository = new ReservationRepository();

    @Override
    protected String getReportName() {
        return "revenue";
    }

    @Override
    protected String getReportPeriod(LocalDate date) {
        return YearMonth.from(date).toString();
    }

    @Override
    protected String generateHeader() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<String> generateRows(LocalDate date) throws Exception {
        List<String> rows = new ArrayList<>();
        LocalDate startOfMonth = date.withDayOfMonth(1);

        var reservations = reservationRepository.findByMonthStarting(startOfMonth);

        for (var reservation : reservations) {
            if (isPaidReservation(reservation.getStatus())) {
                double amount = calculateAmount(reservation);
                rows.add(String.format("%d,%d,%s,%s,%s,%.2f",
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getCheckIn(),
                    reservation.getCheckOut(),
                    reservation.getStatus().name(),
                    amount
                ));
            }
        }

        return rows;
    }

    @Override
    protected String generateTotal(LocalDate date) throws Exception {
        LocalDate startOfMonth = date.withDayOfMonth(1);
        var reservations = reservationRepository.findByMonthStarting(startOfMonth);

        double total = 0;
        for (var reservation : reservations) {
            if (isPaidReservation(reservation.getStatus())) {
                total += calculateAmount(reservation);
            }
        }

        return String.format("total,%.2f", Rounding.round(total));
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
