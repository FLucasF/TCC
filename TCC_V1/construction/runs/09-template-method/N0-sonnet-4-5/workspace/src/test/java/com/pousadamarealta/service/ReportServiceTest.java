package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.repository.MessageRepository;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {
    private ReportService reportService;
    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        RoomRepository roomRepository = new RoomRepository();
        ReservationRepository reservationRepository = new ReservationRepository();
        MessageRepository messageRepository = new MessageRepository();
        PricingService pricingService = new PricingService();

        reservationService = new ReservationService(
                reservationRepository, roomRepository, messageRepository, pricingService);
        reportService = new ReportService(reservationRepository, roomRepository, pricingService);
    }

    @Test
    void testOccupancyReport() {
        var r1 = reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
                2, RatePlan.STANDARD);
        reservationService.confirmReservation(r1.getId());

        var r2 = reservationService.createReservation(
                "201", "Maria Santos", "maria@email.com", "98765432100",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 13),
                3, RatePlan.STANDARD);
        reservationService.confirmReservation(r2.getId());

        String csv = reportService.generateOccupancyReport(LocalDate.of(2027, 7, 10));

        assertTrue(csv.contains("Pousada Maré Alta,occupancy,2027-07-10"));
        assertTrue(csv.contains("room,type,reservation,check_in,check_out"));
        assertTrue(csv.contains("101,STANDARD,1,2027-07-10,2027-07-12"));
        assertTrue(csv.contains("201,DELUXE,2,2027-07-10,2027-07-13"));
        assertTrue(csv.contains("total,2/5"));
    }

    @Test
    void testRevenueReport() {
        var r1 = reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
                2, RatePlan.STANDARD);
        reservationService.confirmReservation(r1.getId());

        var r2 = reservationService.createReservation(
                "201", "Maria Santos", "maria@email.com", "98765432100",
                LocalDate.of(2027, 7, 15), LocalDate.of(2027, 7, 17),
                3, RatePlan.NON_REFUNDABLE);
        reservationService.confirmReservation(r2.getId());
        reservationService.cancelReservation(r2.getId());

        String csv = reportService.generateRevenueReport(YearMonth.of(2027, 7));

        assertTrue(csv.contains("Pousada Maré Alta,revenue,2027-07"));
        assertTrue(csv.contains("reservation,room,check_in,check_out,status,amount"));
        assertTrue(csv.contains("1,101,2027-07-10,2027-07-12,CONFIRMED,599.76"));
        assertTrue(csv.contains("2,201,2027-07-15,2027-07-17,CANCELLED,795.40"));
        assertTrue(csv.contains("total,1395.16"));
    }

    @Test
    void testTourismTaxReport() {
        var r1 = reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
                2, RatePlan.STANDARD);
        reservationService.confirmReservation(r1.getId());

        var r2 = reservationService.createReservation(
                "201", "Maria Santos", "maria@email.com", "98765432100",
                LocalDate.of(2027, 7, 15), LocalDate.of(2027, 7, 18),
                3, RatePlan.STANDARD);
        reservationService.confirmReservation(r2.getId());

        String csv = reportService.generateTourismTaxReport(YearMonth.of(2027, 7));

        assertTrue(csv.contains("Pousada Maré Alta,tourism-tax,2027-07"));
        assertTrue(csv.contains("reservation,room,check_in,check_out,guests,nights,tax"));
        assertTrue(csv.contains("1,101,2027-07-10,2027-07-12,2,2,18.00"));
        assertTrue(csv.contains("2,201,2027-07-15,2027-07-18,3,3,40.50"));
        assertTrue(csv.contains("total,58.50"));
    }

    @Test
    void testRatePlansReport() {
        var r1 = reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
                2, RatePlan.STANDARD);
        reservationService.confirmReservation(r1.getId());

        var r2 = reservationService.createReservation(
                "201", "Maria Santos", "maria@email.com", "98765432100",
                LocalDate.of(2027, 7, 15), LocalDate.of(2027, 7, 17),
                3, RatePlan.NON_REFUNDABLE);
        reservationService.confirmReservation(r2.getId());

        var r3 = reservationService.createReservation(
                "301", "Carlos Lima", "carlos@email.com", "11122233344",
                LocalDate.of(2027, 7, 20), LocalDate.of(2027, 7, 22),
                4, RatePlan.STANDARD);
        reservationService.confirmReservation(r3.getId());

        String csv = reportService.generateRatePlansReport(YearMonth.of(2027, 7));

        assertTrue(csv.contains("Pousada Maré Alta,rate-plans,2027-07"));
        assertTrue(csv.contains("rate_plan,reservations,amount"));
        assertTrue(csv.contains("STANDARD,2,"));
        assertTrue(csv.contains("NON_REFUNDABLE,1,"));
        assertTrue(csv.contains("total,3,"));
    }

    @Test
    void testRatePlansReportWithNoReservationsForOnePlan() {
        var r1 = reservationService.createReservation(
                "101", "João Silva", "joao@email.com", "12345678900",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
                2, RatePlan.STANDARD);
        reservationService.confirmReservation(r1.getId());

        String csv = reportService.generateRatePlansReport(YearMonth.of(2027, 7));

        assertTrue(csv.contains("STANDARD,1,"));
        assertTrue(csv.contains("NON_REFUNDABLE,0,0.00"));
    }
}
