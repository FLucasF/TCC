package com.pousadamarealta.service;

import com.pousadamarealta.report.*;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;

import java.time.LocalDate;
import java.time.YearMonth;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        CsvReport report = new OccupancyReport(date, reservationRepository, roomRepository);
        return report.generate();
    }

    public String generateRevenueReport(YearMonth month) {
        CsvReport report = new RevenueReport(month, reservationRepository);
        return report.generate();
    }

    public String generateTourismTaxReport(YearMonth month) {
        CsvReport report = new TourismTaxReport(month, reservationRepository);
        return report.generate();
    }

    public String generateRatePlansReport(YearMonth month) {
        CsvReport report = new RatePlansReport(month, reservationRepository);
        return report.generate();
    }
}
