package com.pousadamarealta.service;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import com.pousadamarealta.report.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationRepository.findPaidReservationsForNight(date);
        Map<String, Room> roomsByNumber = roomRepository.findAll().stream()
            .collect(Collectors.toMap(Room::number, r -> r));
        int totalRooms = roomRepository.getTotalRoomCount();

        return new OccupancyReport(date, reservations, roomsByNumber, totalRooms).generate();
    }

    public String generateRevenueReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findReservationsStartingInMonth(
            month.getYear(), month.getMonthValue()
        );

        return new RevenueReport(month, reservations).generate();
    }

    public String generateTourismTaxReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findPaidReservationsStartingInMonth(
            month.getYear(), month.getMonthValue()
        );

        return new TourismTaxReport(month, reservations).generate();
    }

    public String generateRatePlansReport(YearMonth month) {
        List<Reservation> reservations = reservationRepository.findReservationsStartingInMonth(
            month.getYear(), month.getMonthValue()
        );

        return new RatePlansReport(month, reservations).generate();
    }
}
