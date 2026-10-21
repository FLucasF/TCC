package com.pousada.service;

import com.pousada.report.*;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.time.LocalDate;
import java.time.YearMonth;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository,
                        RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        Report report = new OccupancyReport(date, reservationRepository, roomRepository);
        return report.generate();
    }

    public String generateRevenueReport(YearMonth month) {
        Report report = new RevenueReport(month, reservationRepository);
        return report.generate();
    }

    public String generateTourismTaxReport(YearMonth month) {
        Report report = new TourismTaxReport(month, reservationRepository);
        return report.generate();
    }

    public String generateRatePlansReport(YearMonth month) {
        Report report = new RatePlansReport(month, reservationRepository);
        return report.generate();
    }
}
