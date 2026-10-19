package com.pousada.service;

import com.pousada.domain.Reservation;
import com.pousada.domain.Room;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.strategy.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final Map<String, ReportStrategy> strategies;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.strategies = new HashMap<>();

        registerStrategy(new OccupancyReportStrategy());
        registerStrategy(new RevenueReportStrategy());
        registerStrategy(new TourismTaxReportStrategy());
        registerStrategy(new RatePlansReportStrategy());
    }

    private void registerStrategy(ReportStrategy strategy) {
        strategies.put(strategy.getReportName(), strategy);
    }

    public String generateOccupancyReport(LocalDate date) {
        ReportStrategy strategy = strategies.get("occupancy");
        List<Reservation> reservations = reservationRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        return strategy.generate(reservations, rooms, date);
    }

    public String generateRevenueReport(YearMonth month) {
        ReportStrategy strategy = strategies.get("revenue");
        List<Reservation> reservations = reservationRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        return strategy.generate(reservations, rooms, month);
    }

    public String generateTourismTaxReport(YearMonth month) {
        ReportStrategy strategy = strategies.get("tourism-tax");
        List<Reservation> reservations = reservationRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        return strategy.generate(reservations, rooms, month);
    }

    public String generateRatePlansReport(YearMonth month) {
        ReportStrategy strategy = strategies.get("rate-plans");
        List<Reservation> reservations = reservationRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        return strategy.generate(reservations, rooms, month);
    }
}
