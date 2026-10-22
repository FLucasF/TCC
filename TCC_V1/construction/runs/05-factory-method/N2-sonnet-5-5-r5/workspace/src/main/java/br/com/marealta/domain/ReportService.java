package br.com.marealta.domain;

import br.com.marealta.persistence.ReservationRepository;

import java.time.LocalDate;
import java.time.YearMonth;

public class ReportService {
    private final ReservationRepository reservations;

    public ReportService(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    public CsvReport occupancy(LocalDate night) {
        return new OccupancyReport(reservations.findCoveringNight(night), night);
    }

    public CsvReport revenue(YearMonth month) {
        return new RevenueReport(
                reservations.findCheckingInBetween(month.atDay(1), month.plusMonths(1).atDay(1)));
    }
}
