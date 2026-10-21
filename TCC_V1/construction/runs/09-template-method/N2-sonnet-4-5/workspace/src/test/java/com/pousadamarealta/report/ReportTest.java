package com.pousadamarealta.report;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.repository.ReservationRepository;
import com.pousadamarealta.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

class ReportTest {
    private ReservationRepository reservationRepository;
    private RoomRepository roomRepository;

    @BeforeEach
    void setUp() {
        reservationRepository = new ReservationRepository();
        roomRepository = new RoomRepository();
    }

    @Test
    void testOccupancyReportStructure() {
        LocalDate date = LocalDate.of(2027, 3, 10);
        OccupancyReport report = new OccupancyReport(date, reservationRepository, roomRepository);

        String csv = report.generate();

        assertTrue(csv.startsWith("Pousada Maré Alta,occupancy,2027-03-10\n"));
        assertTrue(csv.contains("room,type,reservation,check_in,check_out\n"));
        assertTrue(csv.contains("total,"));
    }

    @Test
    void testRevenueReportStructure() {
        YearMonth month = YearMonth.of(2027, 3);
        RevenueReport report = new RevenueReport(month, reservationRepository);

        String csv = report.generate();

        assertTrue(csv.startsWith("Pousada Maré Alta,revenue,2027-03\n"));
        assertTrue(csv.contains("reservation,room,check_in,check_out,status,amount\n"));
        assertTrue(csv.endsWith("total,0.00"));
    }

    @Test
    void testTourismTaxReportStructure() {
        YearMonth month = YearMonth.of(2027, 3);
        TourismTaxReport report = new TourismTaxReport(month, reservationRepository);

        String csv = report.generate();

        assertTrue(csv.startsWith("Pousada Maré Alta,tourism-tax,2027-03\n"));
        assertTrue(csv.contains("reservation,room,check_in,check_out,guests,nights,tax\n"));
        assertTrue(csv.endsWith("total,0.00"));
    }

    @Test
    void testRatePlansReportStructure() {
        YearMonth month = YearMonth.of(2027, 3);
        RatePlansReport report = new RatePlansReport(month, reservationRepository);

        String csv = report.generate();

        assertTrue(csv.startsWith("Pousada Maré Alta,rate-plans,2027-03\n"));
        assertTrue(csv.contains("rate_plan,reservations,amount\n"));
        assertTrue(csv.contains("STANDARD,0,0.00\n"));
        assertTrue(csv.contains("NON_REFUNDABLE,0,0.00\n"));
        assertTrue(csv.endsWith("total,0,0.00"));
    }

    @Test
    void testTourismTaxCalculation() {
        Reservation reservation = reservationRepository.save(new Reservation(
                reservationRepository.nextId(),
                "101",
                roomRepository.findByNumber("101").get().getType(),
                "Test Guest",
                "test@example.com",
                "12345678900",
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 13),
                2,
                RatePlan.STANDARD
        ));
        reservation.confirm();

        YearMonth month = YearMonth.of(2027, 3);
        TourismTaxReport report = new TourismTaxReport(month, reservationRepository);

        String csv = report.generate();

        assertTrue(csv.contains(",2,3,27.00\n"));
        assertTrue(csv.endsWith("total,27.00"));
    }
}
