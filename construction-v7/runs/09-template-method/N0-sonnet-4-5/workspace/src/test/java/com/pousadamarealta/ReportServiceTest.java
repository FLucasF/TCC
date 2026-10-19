package com.pousadamarealta;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceTest {

    private ReportService reportService;
    private ReservationService reservationService;
    private RoomService roomService;
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService();
        messageService = new MessageService();
        reservationService = new ReservationService(roomService, messageService);
        reportService = new ReportService(reservationService, roomService);
    }

    @Test
    void testOccupancyReport() {
        Reservation r1 = reservationService.createReservation(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                2,
                RatePlan.STANDARD
        );
        reservationService.confirmReservation(r1.getId());

        Reservation r2 = reservationService.createReservation(
                "201",
                "Maria Santos",
                "maria@example.com",
                "98765432109",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 11),
                2,
                RatePlan.STANDARD
        );
        reservationService.confirmReservation(r2.getId());

        String report = reportService.generateOccupancyReport(LocalDate.of(2027, 7, 10));

        assertTrue(report.contains("Pousada Maré Alta,occupancy,2027-07-10"));
        assertTrue(report.contains("room,type,reservation,check_in,check_out"));
        assertTrue(report.contains("101,STANDARD,1,2027-07-10,2027-07-12"));
        assertTrue(report.contains("201,DELUXE,2,2027-07-10,2027-07-11"));
        assertTrue(report.contains("total,2/5"));
    }

    @Test
    void testRevenueReport() {
        Reservation r1 = reservationService.createReservation(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                2,
                RatePlan.STANDARD
        );
        reservationService.confirmReservation(r1.getId());

        Reservation r2 = reservationService.createReservation(
                "201",
                "Maria Santos",
                "maria@example.com",
                "98765432109",
                LocalDate.of(2027, 7, 15),
                LocalDate.of(2027, 7, 16),
                2,
                RatePlan.NON_REFUNDABLE
        );
        reservationService.confirmReservation(r2.getId());
        reservationService.cancelReservation(r2.getId());

        String report = reportService.generateRevenueReport(YearMonth.of(2027, 7));

        assertTrue(report.contains("Pousada Maré Alta,revenue,2027-07"));
        assertTrue(report.contains("reservation,room,check_in,check_out,status,amount"));
        assertTrue(report.contains("1,101,2027-07-10,2027-07-12,CONFIRMED,599.76"));
        assertTrue(report.contains("2,201,2027-07-15,2027-07-16,CANCELLED,397.70"));
    }

    @Test
    void testTourismTaxReport() {
        Reservation r1 = reservationService.createReservation(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 13),
                2,
                RatePlan.STANDARD
        );
        reservationService.confirmReservation(r1.getId());

        String report = reportService.generateTourismTaxReport(YearMonth.of(2027, 7));

        assertTrue(report.contains("Pousada Maré Alta,tourism-tax,2027-07"));
        assertTrue(report.contains("reservation,room,check_in,check_out,guests,nights,tax"));
        assertTrue(report.contains("1,101,2027-07-10,2027-07-13,2,3,27.00"));
        assertTrue(report.contains("total,27.00"));
    }

    @Test
    void testRatePlansReport() {
        Reservation r1 = reservationService.createReservation(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                2,
                RatePlan.STANDARD
        );
        reservationService.confirmReservation(r1.getId());

        Reservation r2 = reservationService.createReservation(
                "201",
                "Maria Santos",
                "maria@example.com",
                "98765432109",
                LocalDate.of(2027, 7, 15),
                LocalDate.of(2027, 7, 16),
                2,
                RatePlan.NON_REFUNDABLE
        );
        reservationService.confirmReservation(r2.getId());

        String report = reportService.generateRatePlansReport(YearMonth.of(2027, 7));

        assertTrue(report.contains("Pousada Maré Alta,rate-plans,2027-07"));
        assertTrue(report.contains("rate_plan,reservations,amount"));
        assertTrue(report.contains("STANDARD,1,599.76"));
        assertTrue(report.contains("NON_REFUNDABLE,1,397.70"));
        assertTrue(report.contains("total,2,997.46"));
    }

    @Test
    void testRatePlansReportWithEmptyPlan() {
        Reservation r1 = reservationService.createReservation(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                2,
                RatePlan.STANDARD
        );
        reservationService.confirmReservation(r1.getId());

        String report = reportService.generateRatePlansReport(YearMonth.of(2027, 7));

        assertTrue(report.contains("STANDARD,1,599.76"));
        assertTrue(report.contains("NON_REFUNDABLE,0,0.00"));
        assertTrue(report.contains("total,1,599.76"));
    }
}
