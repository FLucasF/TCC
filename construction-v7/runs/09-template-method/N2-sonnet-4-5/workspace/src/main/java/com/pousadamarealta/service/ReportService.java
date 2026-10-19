package com.pousadamarealta.service;

import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.service.report.*;

import java.util.HashMap;
import java.util.Map;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final Map<String, ReportGenerator> generators;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.generators = new HashMap<>();

        generators.put("occupancy", new OccupancyReportGenerator());
        generators.put("revenue", new RevenueReportGenerator());
        generators.put("tourism-tax", new TourismTaxReportGenerator());
        generators.put("rate-plans", new RatePlansReportGenerator());
    }

    public String generateReport(String reportType, String parameter) {
        ReportGenerator generator = generators.get(reportType);
        if (generator == null) {
            throw new IllegalArgumentException("Unknown report type: " + reportType);
        }

        return generator.generate(
            reservationRepository.findAll(),
            roomRepository.findAll(),
            parameter
        );
    }
}
