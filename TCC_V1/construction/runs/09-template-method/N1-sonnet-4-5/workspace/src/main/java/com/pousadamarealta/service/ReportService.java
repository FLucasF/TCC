package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.infrastructure.ReservationRepository;
import com.pousadamarealta.service.report.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {
    private static final int TOTAL_ROOMS = 5;

    private final ReservationRepository reservationRepository;

    public ReportService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findByNight(date);
        OccupancyReport report = new OccupancyReport(date, reservations, TOTAL_ROOMS);
        return report.generate();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByMonth(month);
        RevenueReport report = new RevenueReport(month, reservations);
        return report.generate();
    }

    public String generateTourismTaxReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByMonth(month);
        TourismTaxReport report = new TourismTaxReport(month, reservations);
        return report.generate();
    }

    public String generateRatePlansReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findByMonth(month);
        RatePlansReport report = new RatePlansReport(month, reservations);
        return report.generate();
    }
}
