package com.pousadamarealta;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReportServiceTest {
    private ReportService reportService;
    private ReservationService reservationService;

    @BeforeEach
    public void setUp() {
        MessageService messageService = new MessageService(false);
        reservationService = new ReservationService(messageService);
        reportService = new ReportService(reservationService);
    }

    @Test
    public void testOccupancyReport() {
        reservationService.createReservation(101, "João", "joao@example.com", "12345678901",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD);

        reservationService.confirmReservation(1, 499.80);

        String report = reportService.generateOccupancyReport(LocalDate.of(2027, 3, 10));

        assertTrue(report.contains("room,type,reservation,check_in,check_out"));
        assertTrue(report.contains("101,STANDARD,1,2027-03-10,2027-03-12"));
        assertTrue(report.contains("total,1/5"));
    }

    @Test
    public void testRevenueReport() {
        reservationService.createReservation(101, "João", "joao@example.com", "12345678901",
                LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), 2, RatePlan.STANDARD);

        reservationService.confirmReservation(1, 599.76);

        String report = reportService.generateRevenueReport(YearMonth.of(2027, 7));

        assertTrue(report.contains("reservation,room,check_in,check_out,status,amount"));
        assertTrue(report.contains("1,101,2027-07-10,2027-07-12,CONFIRMED,599.76"));
        assertTrue(report.contains("total,599.76"));
    }
}
