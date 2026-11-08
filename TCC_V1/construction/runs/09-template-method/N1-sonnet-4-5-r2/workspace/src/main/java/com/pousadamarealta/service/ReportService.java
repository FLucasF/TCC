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
        return new OccupancyReport(date, reservationRepository, roomRepository).generate();
    }

    public String generateRevenueReport(YearMonth month) {
        return new RevenueReport(month, reservationRepository).generate();
    }

    public String generateTourismTaxReport(YearMonth month) {
        return new TourismTaxReport(month, reservationRepository).generate();
    }

    public String generateRatePlanReport(YearMonth month) {
        return new RatePlanReport(month, reservationRepository).generate();
    }
}
