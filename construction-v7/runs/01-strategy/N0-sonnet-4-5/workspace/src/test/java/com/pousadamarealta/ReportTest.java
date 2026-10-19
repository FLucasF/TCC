package com.pousadamarealta;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.service.MessageService;
import com.pousadamarealta.service.ReportService;
import com.pousadamarealta.service.ReservationService;
import com.pousadamarealta.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class ReportTest {

    private ReservationService reservationService;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        RoomService roomService = new RoomService();
        MessageService messageService = new MessageService();
        reservationService = new ReservationService(roomService, messageService);
        reportService = new ReportService(reservationService, roomService);
    }

    @Test
    void testOccupancyReport() {
        var r1 = reservationService.createReservation(
            "101", "Guest 1", "g1@test.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD, null
        );
        reservationService.confirmReservation(r1.getId());

        var r2 = reservationService.createReservation(
            "201", "Guest 2", "g2@test.com", "12345678902",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 15),
            2, RatePlan.STANDARD, null
        );
        reservationService.confirmReservation(r2.getId());

        String report = reportService.generateOccupancyReport(LocalDate.of(2027, 7, 10));

        assertThat(report).contains("room,type,reservation,check_in,check_out");
        assertThat(report).contains("101,STANDARD,1,2027-07-10,2027-07-12");
        assertThat(report).contains("201,DELUXE,2,2027-07-10,2027-07-15");
        assertThat(report).contains("total,2/5");
    }

    @Test
    void testRevenueReport() {
        var r1 = reservationService.createReservation(
            "101", "Guest 1", "g1@test.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD, null
        );
        reservationService.confirmReservation(r1.getId());

        var r2 = reservationService.createReservation(
            "201", "Guest 2", "g2@test.com", "12345678902",
            LocalDate.of(2027, 3, 15), LocalDate.of(2027, 3, 17),
            2, RatePlan.NON_REFUNDABLE, null
        );
        reservationService.confirmReservation(r2.getId());
        reservationService.cancelReservation(r2.getId());

        String report = reportService.generateRevenueReport(YearMonth.of(2027, 3));

        assertThat(report).contains("reservation,room,check_in,check_out,status,amount");
        assertThat(report).contains("1,101,2027-03-10,2027-03-12,CONFIRMED,499.80");
        assertThat(report).contains("2,201,2027-03-15,2027-03-17,CANCELLED,662.83");
        assertThat(report).contains("total,1162.63");
    }

    @Test
    void testOccupancyReportExcludesCancelled() {
        var r1 = reservationService.createReservation(
            "101", "Guest 1", "g1@test.com", "12345678901",
            LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
            2, RatePlan.STANDARD, null
        );
        reservationService.confirmReservation(r1.getId());
        reservationService.cancelReservation(r1.getId());

        String report = reportService.generateOccupancyReport(LocalDate.of(2027, 7, 10));

        assertThat(report).contains("total,0/5");
        assertThat(report).doesNotContain("101,STANDARD");
    }

    @Test
    void testRevenueReportOnlyIncludesReservationsWithRevenue() {
        var r1 = reservationService.createReservation(
            "101", "Guest 1", "g1@test.com", "12345678901",
            LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
            2, RatePlan.STANDARD, null
        );

        String report = reportService.generateRevenueReport(YearMonth.of(2027, 3));

        assertThat(report).doesNotContain("1,101");
        assertThat(report).contains("total,0.00");
    }
}
