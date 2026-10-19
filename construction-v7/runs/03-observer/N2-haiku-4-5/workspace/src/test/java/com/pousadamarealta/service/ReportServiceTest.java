package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

public class ReportServiceTest {
    private ReportService reportService;
    private ReservationService reservationService;
    private ReservationRepository reservationRepo;

    @BeforeEach
    public void setup() {
        reservationRepo = new ReservationRepository();
        RoomRepository roomRepo = new RoomRepository();
        MessageRepository messageRepo = new MessageRepository();
        PricingService pricingService = new PricingService();
        reservationService = new ReservationService(reservationRepo, roomRepo, messageRepo, pricingService);
        reportService = new ReportService(reservationRepo, roomRepo);
    }

    @Test
    public void testOccupancyReportEmpty() {
        String report = reportService.generateOccupancyReport(LocalDate.of(2027, 3, 15));

        assertTrue(report.contains("room,type,reservation,check_in,check_out"));
        assertTrue(report.contains("total,0/5"));
    }

    @Test
    public void testOccupancyReportWithReservations() {
        Reservation res1 = reservationService.createReservation(
            101, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(res1.getId());

        Reservation res2 = reservationService.createReservation(
            201, "Maria Santos", "maria@example.com", "12345678902",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(res2.getId());

        String report = reportService.generateOccupancyReport(LocalDate.of(2027, 3, 11));

        assertTrue(report.contains("101,STANDARD,1,2027-03-10,2027-03-12"));
        assertTrue(report.contains("201,DELUXE,2,2027-03-10,2027-03-12"));
        assertTrue(report.contains("total,2/5"));
    }

    @Test
    public void testOccupancyReportIgnoresCheckOutDate() {
        Reservation res = reservationService.createReservation(
            101, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(res.getId());

        String report = reportService.generateOccupancyReport(LocalDate.of(2027, 3, 12));

        assertFalse(report.contains("101,STANDARD,1"));
        assertTrue(report.contains("total,0/5"));
    }

    @Test
    public void testRevenueReportEmpty() {
        String report = reportService.generateRevenueReport(YearMonth.of(2027, 3));

        assertTrue(report.contains("reservation,room,check_in,check_out,status,amount"));
        assertTrue(report.contains("total,,,,0.00"));
    }

    @Test
    public void testRevenueReportWithReservations() {
        Reservation res = reservationService.createReservation(
            101, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(res.getId());

        String report = reportService.generateRevenueReport(YearMonth.of(2027, 3));

        assertTrue(report.contains("1,101,2027-03-10,2027-03-12,CONFIRMED,499.80"));
        assertTrue(report.contains("total,,,,499.80"));
    }

    @Test
    public void testRevenueReportWithCancellation() {
        Reservation res = reservationService.createReservation(
            101, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(res.getId());
        reservationService.cancelReservation(res.getId());

        String report = reportService.generateRevenueReport(YearMonth.of(2027, 3));

        assertTrue(report.contains("CANCELLED"));
        assertTrue(report.contains("total,,,,0.00"));
    }

    @Test
    public void testRevenueReportWithNonRefundableCancellation() {
        Reservation res = reservationService.createReservation(
            101, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.NON_REFUNDABLE
        );
        reservationService.confirmReservation(res.getId());
        reservationService.cancelReservation(res.getId());

        String report = reportService.generateRevenueReport(YearMonth.of(2027, 3));

        assertTrue(report.contains("CANCELLED"));
        assertTrue(report.contains("total,,,,424.83"));
    }

    @Test
    public void testRevenueReportFiltersOtherMonths() {
        Reservation res = reservationService.createReservation(
            101, "João Silva", "joao@example.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD
        );
        reservationService.confirmReservation(res.getId());

        String report = reportService.generateRevenueReport(YearMonth.of(2027, 4));

        assertTrue(report.contains("total,,,,0.00"));
    }
}
