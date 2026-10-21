package hotel.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.reservations.Guest;
import hotel.reservations.NewReservation;
import hotel.reservations.PriceCalculator;
import hotel.reservations.RatePlan;
import hotel.reservations.Reservation;
import hotel.reservations.ReservationService;
import hotel.reservations.ReservationStore;
import hotel.rooms.Rooms;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReportsTest {

    private final Rooms rooms = new Rooms();
    private final ReservationStore store = new ReservationStore();
    private final ReservationService service =
            new ReservationService(rooms, store, new PriceCalculator(), new EmailSender(new SentMessages()));

    private Reservation book(int roomNumber, String checkIn, String checkOut, RatePlan plan) {
        Guest guest = new Guest("Bruno Lima", "bruno@example.com", "987.654.321-00");
        return service.create(new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 1, plan));
    }

    @BeforeEach
    void someReservations() {
        Reservation april = book(201, "2027-04-01", "2027-04-03", RatePlan.STANDARD);
        Reservation lateMarch = book(101, "2027-03-30", "2027-04-02", RatePlan.NON_REFUNDABLE);
        Reservation suite = book(301, "2027-04-02", "2027-04-04", RatePlan.STANDARD);
        book(102, "2027-04-01", "2027-04-05", RatePlan.STANDARD); // never paid
        Reservation cancelled = book(202, "2027-04-01", "2027-04-02", RatePlan.STANDARD);
        service.confirm(april.id());
        service.confirm(lateMarch.id());
        service.confirm(suite.id());
        service.checkIn(suite.id());
        service.confirm(cancelled.id());
        service.cancel(cancelled.id());
    }

    @Test
    void occupancyListsThePaidReservationsCoveringTheNight() {
        String csv = new OccupancyReport(rooms, store).generate(LocalDate.of(2027, 4, 1));

        assertEquals("""
                Pousada Maré Alta,occupancy,2027-04-01
                room,type,reservation,check_in,check_out
                101,STANDARD,2,2027-03-30,2027-04-02
                201,DELUXE,1,2027-04-01,2027-04-03
                total,2/5
                """, csv);
    }

    @Test
    void theCheckOutNightIsNotOccupied() {
        String csv = new OccupancyReport(rooms, store).generate(LocalDate.of(2027, 4, 3));

        assertEquals("""
                Pousada Maré Alta,occupancy,2027-04-03
                room,type,reservation,check_in,check_out
                301,SUITE,3,2027-04-02,2027-04-04
                total,1/5
                """, csv);
    }

    @Test
    void revenueSumsTheMoneyKeptFromReservationsStartingInTheMonth() {
        String csv = new RevenueReport(store).generate(YearMonth.of(2027, 4));

        // the unpaid reservation and the refunded cancellation bring nothing
        assertEquals("""
                Pousada Maré Alta,revenue,2027-04
                reservation,room,check_in,check_out,status,amount
                1,201,2027-04-01,2027-04-03,CONFIRMED,779.80
                3,301,2027-04-02,2027-04-04,CHECKED_IN,1159.80
                total,1939.60
                """, csv);
    }

    @Test
    void aCancelledNonRefundableReservationStillCountsAsRevenue() {
        Reservation may = book(101, "2027-05-10", "2027-05-11", RatePlan.NON_REFUNDABLE);
        service.confirm(may.id());
        service.cancel(may.id());

        String csv = new RevenueReport(store).generate(YearMonth.of(2027, 5));

        assertEquals("""
                Pousada Maré Alta,revenue,2027-05
                reservation,room,check_in,check_out,status,amount
                6,101,2027-05-10,2027-05-11,CANCELLED,212.42
                total,212.42
                """, csv);
    }

    @Test
    void anEmptyMonthHasOnlyTheTotal() {
        String csv = new RevenueReport(store).generate(YearMonth.of(2027, 6));

        assertEquals("""
                Pousada Maré Alta,revenue,2027-06
                reservation,room,check_in,check_out,status,amount
                total,0.00
                """, csv);
    }

    @Test
    void tourismTaxReportCalculatesTaxPerGuestPerNight() {
        String csv = new TourismTaxReport(store).generate(YearMonth.of(2027, 4));

        // reservation 1: 1 guest, 2 nights (apr 1-3) = 1 * 2 * 4.50 = 9.00
        // reservation 3: 1 guest, 2 nights (apr 2-4) = 1 * 2 * 4.50 = 9.00
        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-04
                reservation,room,check_in,check_out,guests,nights,tax
                1,201,2027-04-01,2027-04-03,1,2,9.00
                3,301,2027-04-02,2027-04-04,1,2,9.00
                total,18.00
                """, csv);
    }

    @Test
    void tourismTaxReportOnlyIncludesPaidReservations() {
        String csv = new TourismTaxReport(store).generate(YearMonth.of(2027, 4));

        // The unpaid and cancelled reservations are excluded
        String totalLine = csv.split("\n")[csv.split("\n").length - 1];
        assertEquals("18.00", totalLine.split(",")[1]);
    }

    @Test
    void tourismTaxReportEmptyMonthHasOnlyTotal() {
        String csv = new TourismTaxReport(store).generate(YearMonth.of(2027, 6));

        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-06
                reservation,room,check_in,check_out,guests,nights,tax
                total,0.00
                """, csv);
    }

    @Test
    void ratePlanReportShowsBothPlans() {
        String csv = new RatePlanReport(store).generate(YearMonth.of(2027, 4));

        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-04
                rate_plan,reservations,amount
                STANDARD,2,1939.60
                NON_REFUNDABLE,0,0.00
                total,2,1939.60
                """, csv);
    }

    @Test
    void ratePlanReportIncludesEmptyPlans() {
        Reservation mayReservation = book(101, "2027-05-10", "2027-05-11", RatePlan.STANDARD);
        service.confirm(mayReservation.id());

        String csv = new RatePlanReport(store).generate(YearMonth.of(2027, 5));

        assertTrue(csv.contains("STANDARD,1,"));
        assertTrue(csv.contains("NON_REFUNDABLE,0,0.00"));
        assertTrue(csv.contains("total,1,"));
    }

    @Test
    void ratePlanReportEmptyMonthHasBothPlanLines() {
        String csv = new RatePlanReport(store).generate(YearMonth.of(2027, 6));

        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-06
                rate_plan,reservations,amount
                STANDARD,0,0.00
                NON_REFUNDABLE,0,0.00
                total,0,0.00
                """, csv);
    }
}
