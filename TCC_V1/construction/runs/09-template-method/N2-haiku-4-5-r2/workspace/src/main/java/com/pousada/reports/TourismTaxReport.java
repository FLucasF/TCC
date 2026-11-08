package com.pousada.reports;

import com.pousada.domain.ReservationStatus;
import com.pousada.domain.Rounding;
import com.pousada.infrastructure.ReservationRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class TourismTaxReport extends Report {
    private static final double TOURISM_TAX_PER_GUEST_PER_NIGHT = 4.50;
    private final ReservationRepository reservationRepository = new ReservationRepository();

    @Override
    protected String getReportName() {
        return "tourism-tax";
    }

    @Override
    protected String getReportPeriod(LocalDate date) {
        return YearMonth.from(date).toString();
    }

    @Override
    protected String generateHeader() {
        return "reservation,room,check_in,check_out,guests,nights,tax";
    }

    @Override
    protected List<String> generateRows(LocalDate date) throws Exception {
        List<String> rows = new ArrayList<>();
        LocalDate startOfMonth = date.withDayOfMonth(1);

        var reservations = reservationRepository.findByMonthStarting(startOfMonth);

        for (var reservation : reservations) {
            if (isPaidReservation(reservation.getStatus())) {
                double tax = calculateTax(reservation);
                rows.add(String.format("%d,%d,%s,%s,%d,%d,%.2f",
                    reservation.getId(),
                    reservation.getRoomNumber(),
                    reservation.getCheckIn(),
                    reservation.getCheckOut(),
                    reservation.getGuests(),
                    reservation.getNights(),
                    tax
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
                total += calculateTax(reservation);
            }
        }

        return String.format("total,%.2f", Rounding.round(total));
    }

    private boolean isPaidReservation(ReservationStatus status) {
        return status == ReservationStatus.CONFIRMED ||
               status == ReservationStatus.CHECKED_IN ||
               status == ReservationStatus.CHECKED_OUT;
    }

    private double calculateTax(com.pousada.domain.Reservation reservation) {
        return Rounding.round(reservation.getGuests() * reservation.getNights() * TOURISM_TAX_PER_GUEST_PER_NIGHT);
    }
}
