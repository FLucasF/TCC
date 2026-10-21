package com.pousadamarealta.service;

import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import java.time.LocalDate;
import java.time.YearMonth;

public class ReportService {
    private final ReservationRepository resRepo;
    private final RoomRepository roomRepo;

    public ReportService(ReservationRepository resRepo, RoomRepository roomRepo) {
        this.resRepo = resRepo;
        this.roomRepo = roomRepo;
    }

    public String generateOccupancyReport(LocalDate date) {
        ReportStrategy strategy = new OccupancyReportStrategy();
        return strategy.generate(resRepo, roomRepo, date);
    }

    public String generateRevenueReport(YearMonth yearMonth) {
        ReportStrategy strategy = new RevenueReportStrategy();
        return strategy.generate(resRepo, roomRepo, yearMonth);
    }
}
