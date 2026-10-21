package com.pousada.service;

import com.pousada.model.RatePlan;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {
    private ReportService reportService;
    private ReservationService reservationService;
    private ReservationRepository reservationRepository;
    private RoomRepository roomRepository;

    @BeforeEach
    void setUp() {
        reservationRepository = new ReservationRepository();
        roomRepository = new RoomRepository();
        reservationService = new ReservationService(reservationRepository, roomRepository);
        reportService = new ReportService(reservationRepository, roomRepository);
    }

    @Test
    void testOccupancyReport() {
        // Create a confirmed reservation
        var r = reservationService.createReservation(
            101, "João", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(r.getId());

        String report = reportService.generateOccupancyReport(LocalDate.of(2027, 7, 10));

        assertTrue(report.contains("Pousada Maré Alta,occupancy,2027-07-10"));
        assertTrue(report.contains("room,type,reservation,check_in,check_out"));
        assertTrue(report.contains("101,STANDARD,1,2027-07-10,2027-07-12"));
        assertTrue(report.contains("total,1/5"));
    }

    @Test
    void testRevenueReport() {
        // Create a confirmed reservation for July 2027
        var r = reservationService.createReservation(
            101, "João", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(r.getId());

        String report = reportService.generateRevenueReport(YearMonth.of(2027, 7));

        assertTrue(report.contains("Pousada Maré Alta,revenue,2027-07"));
        assertTrue(report.contains("reservation,room,check_in,check_out,status,amount"));
        assertTrue(report.contains("101"));
        assertTrue(report.contains("599.76"));
    }

    @Test
    void testTourismTaxReport() {
        // Create a confirmed reservation with 2 guests for 2 nights
        // Tax should be R$ 4.50 * 2 guests * 2 nights = R$ 18.00
        var r = reservationService.createReservation(
            101, "João", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(r.getId());

        String report = reportService.generateTourismTaxReport(YearMonth.of(2027, 7));

        assertTrue(report.contains("Pousada Maré Alta,tourism-tax,2027-07"));
        assertTrue(report.contains("reservation,room,check_in,check_out,guests,nights,tax"));
        assertTrue(report.contains("18.00"));
    }

    @Test
    void testRatePlansReport() {
        // Create a STANDARD reservation
        var r1 = reservationService.createReservation(
            101, "João", "joao@example.com", "12345678901",
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(r1.getId());

        // Create a NON_REFUNDABLE reservation
        var r2 = reservationService.createReservation(
            102, "Maria", "maria@example.com", "11122233344",
            LocalDate.of(2027, 7, 15),
            LocalDate.of(2027, 7, 16),
            2, RatePlan.NON_REFUNDABLE
        );
        reservationService.confirmReservation(r2.getId());

        String report = reportService.generateRatePlansReport(YearMonth.of(2027, 7));

        assertTrue(report.contains("Pousada Maré Alta,rate-plans,2027-07"));
        assertTrue(report.contains("rate_plan,reservations,amount"));
        assertTrue(report.contains("STANDARD,1,599.76"));
        assertTrue(report.contains("NON_REFUNDABLE,1"));
    }
}
