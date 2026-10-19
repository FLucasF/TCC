package com.pousada.domain.service;

import com.pousada.infrastructure.repository.ReservationRepository;
import com.pousada.infrastructure.repository.RoomRepository;
import com.pousada.report.ReportGenerator;

import java.util.HashMap;
import java.util.Map;

public class ReportService {
    private final Map<String, ReportGenerator> generators = new HashMap<>();
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public void registerGenerator(ReportGenerator generator) {
        generators.put(generator.getName(), generator);
    }

    public String generateReport(String reportName, String period) {
        ReportGenerator generator = generators.get(reportName);
        if (generator == null) {
            throw new IllegalArgumentException("Unknown report: " + reportName);
        }

        return generator.generate(period, reservationRepository.findAll(), roomRepository);
    }
}
