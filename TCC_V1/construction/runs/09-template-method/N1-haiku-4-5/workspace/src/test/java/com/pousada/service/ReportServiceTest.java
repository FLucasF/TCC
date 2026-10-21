package com.pousada.service;

import com.pousada.domain.RatePlan;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.Assert.*;

public class ReportServiceTest {
    private ReportService reportService;
    private ReservationService reservationService;
    private ReservationRepository reservationRepo;
    private RoomRepository roomRepo;
    private MessageRepository messageRepo;

    @Before
    public void setUp() {
        reservationRepo = new ReservationRepository();
        roomRepo = new RoomRepository();
        messageRepo = new MessageRepository();
        reportService = new ReportService(reservationRepo, roomRepo);
        reservationService = new ReservationService(reservationRepo, roomRepo, messageRepo);
    }

    @Test
    public void testOccupancyReport() {
        var res1 = reservationService.createReservation(101, "Guest 1", "g1@test.com", "111.111.111-11",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 13), 2, RatePlan.STANDARD);
        reservationService.confirmReservation(res1.getId(), res1.getTotal());

        var res2 = reservationService.createReservation(102, "Guest 2", "g2@test.com", "222.222.222-22",
                LocalDate.of(2027, 7, 12), LocalDate.of(2027, 7, 14), 2, RatePlan.STANDARD);
        reservationService.confirmReservation(res2.getId(), res2.getTotal());

        String report = reportService.generateOccupancyReport(LocalDate.of(2027, 7, 11));
        assertTrue(report.contains("Pousada Maré Alta,occupancy,2027-07-11"));
        assertTrue(report.contains("room,type,reservation,check_in,check_out"));
        assertTrue(report.contains("101,STANDARD,1,2027-07-10,2027-07-13"));
        assertTrue(report.contains("total,1/5"));
    }

    @Test
    public void testRevenueReport() {
        var res1 = reservationService.createReservation(101, "Guest 1", "g1@test.com", "111.111.111-11",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 2, RatePlan.STANDARD);
        reservationService.confirmReservation(res1.getId(), res1.getTotal());

        var res2 = reservationService.createReservation(101, "Guest 2", "g2@test.com", "222.222.222-22",
                LocalDate.of(2027, 8, 10), LocalDate.of(2027, 8, 12), 2, RatePlan.STANDARD);
        reservationService.confirmReservation(res2.getId(), res2.getTotal());

        String report = reportService.generateRevenueReport(YearMonth.of(2027, 7));
        assertTrue(report.contains("Pousada Maré Alta,revenue,2027-07"));
        assertTrue(report.contains("1,101,2027-07-10,2027-07-12,CONFIRMED"));
        assertFalse(report.contains("2,101,2027-08-10"));
    }

    @Test
    public void testTourismTaxReport() {
        var res1 = reservationService.createReservation(101, "Guest 1", "g1@test.com", "111.111.111-11",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 13), 2, RatePlan.STANDARD);
        reservationService.confirmReservation(res1.getId(), res1.getTotal());

        String report = reportService.generateTourismTaxReport(YearMonth.of(2027, 7));
        assertTrue(report.contains("Pousada Maré Alta,tourism-tax,2027-07"));
        assertTrue(report.contains("1,101,2027-07-10,2027-07-13,2,3,27.00"));
        assertTrue(report.contains("total,"));
    }

    @Test
    public void testRatePlansReport() {
        var res1 = reservationService.createReservation(101, "Guest 1", "g1@test.com", "111.111.111-11",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 2, RatePlan.STANDARD);
        reservationService.confirmReservation(res1.getId(), res1.getTotal());

        var res2 = reservationService.createReservation(102, "Guest 2", "g2@test.com", "222.222.222-22",
                LocalDate.of(2027, 7, 12), LocalDate.of(2027, 7, 14), 2, RatePlan.NON_REFUNDABLE);
        reservationService.confirmReservation(res2.getId(), res2.getTotal());

        String report = reportService.generateRatePlansReport(YearMonth.of(2027, 7));
        assertTrue(report.contains("Pousada Maré Alta,rate-plans,2027-07"));
        assertTrue(report.contains("STANDARD"));
        assertTrue(report.contains("NON_REFUNDABLE"));
    }
}
