package com.pousadamarealta.service.report;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReportTest {

    @Test
    public void testOccupancyReportFormat() {
        List<Reservation> reservations = new ArrayList<>();

        Reservation r1 = Reservation.create(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                2,
                RatePlan.STANDARD
        );
        r1.setId(1L);
        r1.confirm();

        reservations.add(r1);

        OccupancyReport report = new OccupancyReport(LocalDate.of(2027, 7, 10), reservations, 5);
        String csv = report.generate();

        assertTrue(csv.contains("Pousada Maré Alta,occupancy,2027-07-10"));
        assertTrue(csv.contains("room,type,reservation,check_in,check_out"));
        assertTrue(csv.contains("101,STANDARD,1,2027-07-10,2027-07-12"));
        assertTrue(csv.contains("total,1/5"));
    }

    @Test
    public void testRevenueReportFormat() {
        List<Reservation> reservations = new ArrayList<>();

        Reservation r1 = Reservation.create(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                2,
                RatePlan.STANDARD
        );
        r1.setId(1L);
        r1.confirm();
        r1.checkIn();
        r1.checkOut();

        reservations.add(r1);

        RevenueReport report = new RevenueReport(YearMonth.of(2027, 7), reservations);
        String csv = report.generate();

        assertTrue(csv.contains("Pousada Maré Alta,revenue,2027-07"));
        assertTrue(csv.contains("reservation,room,check_in,check_out,status,amount"));
        assertTrue(csv.contains("1,101,2027-07-10,2027-07-12,CHECKED_OUT,599.76"));
        assertTrue(csv.contains("total,599.76"));
    }

    @Test
    public void testTourismTaxReportFormat() {
        List<Reservation> reservations = new ArrayList<>();

        Reservation r1 = Reservation.create(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                2,
                RatePlan.STANDARD
        );
        r1.setId(1L);
        r1.confirm();

        reservations.add(r1);

        TourismTaxReport report = new TourismTaxReport(YearMonth.of(2027, 7), reservations);
        String csv = report.generate();

        assertTrue(csv.contains("Pousada Maré Alta,tourism-tax,2027-07"));
        assertTrue(csv.contains("reservation,room,check_in,check_out,guests,nights,tax"));
        assertTrue(csv.contains("1,101,2027-07-10,2027-07-12,2,2,18.00"));
        assertTrue(csv.contains("total,18.00"));
    }

    @Test
    public void testRatePlansReportFormat() {
        List<Reservation> reservations = new ArrayList<>();

        Reservation r1 = Reservation.create(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                2,
                RatePlan.STANDARD
        );
        r1.setId(1L);
        r1.confirm();

        Reservation r2 = Reservation.create(
                "201",
                "Maria Santos",
                "maria@example.com",
                "98765432100",
                LocalDate.of(2027, 7, 15),
                LocalDate.of(2027, 7, 17),
                2,
                RatePlan.NON_REFUNDABLE
        );
        r2.setId(2L);
        r2.confirm();

        reservations.add(r1);
        reservations.add(r2);

        RatePlansReport report = new RatePlansReport(YearMonth.of(2027, 7), reservations);
        String csv = report.generate();

        assertTrue(csv.contains("Pousada Maré Alta,rate-plans,2027-07"));
        assertTrue(csv.contains("rate_plan,reservations,amount"));
        assertTrue(csv.contains("STANDARD,1,599.76"));
        assertTrue(csv.contains("NON_REFUNDABLE,1,"));
    }

    @Test
    public void testRevenueReportExcludesPending() {
        List<Reservation> reservations = new ArrayList<>();

        Reservation r1 = Reservation.create(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                2,
                RatePlan.STANDARD
        );
        r1.setId(1L);

        reservations.add(r1);

        RevenueReport report = new RevenueReport(YearMonth.of(2027, 7), reservations);
        String csv = report.generate();

        assertFalse(csv.contains("1,101"));
        assertTrue(csv.contains("total,0.00"));
    }

    @Test
    public void testTourismTaxReportExcludesPending() {
        List<Reservation> reservations = new ArrayList<>();

        Reservation r1 = Reservation.create(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                2,
                RatePlan.STANDARD
        );
        r1.setId(1L);

        reservations.add(r1);

        TourismTaxReport report = new TourismTaxReport(YearMonth.of(2027, 7), reservations);
        String csv = report.generate();

        assertFalse(csv.contains("1,101"));
        assertTrue(csv.contains("total,0.00"));
    }

    @Test
    public void testRatePlansReportShowsAllPlans() {
        List<Reservation> reservations = new ArrayList<>();

        Reservation r1 = Reservation.create(
                "101",
                "João Silva",
                "joao@example.com",
                "12345678901",
                LocalDate.of(2027, 7, 10),
                LocalDate.of(2027, 7, 12),
                2,
                RatePlan.STANDARD
        );
        r1.setId(1L);
        r1.confirm();

        reservations.add(r1);

        RatePlansReport report = new RatePlansReport(YearMonth.of(2027, 7), reservations);
        String csv = report.generate();

        assertTrue(csv.contains("STANDARD,1,"));
        assertTrue(csv.contains("NON_REFUNDABLE,0,0.00"));
    }
}
