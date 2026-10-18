package hotel.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    void tourismTaxCalculatesTheTaxForPaidReservations() {
        String csv = new TourismTaxReport(store).generate(YearMonth.of(2027, 4));

        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-04
                reservation,room,check_in,check_out,guests,nights,tax
                1,201,2027-04-01,2027-04-03,1,2,9.00
                3,301,2027-04-02,2027-04-04,1,2,9.00
                total,18.00
                """, csv);
    }

    @Test
    void tourismTaxMultipliesGuestsAndNights() {
        Guest guest = new Guest("Ana Costa", "ana@example.com", "123.456.789-00");
        Reservation june = service.create(
                new NewReservation(201, guest, LocalDate.parse("2027-06-10"), LocalDate.parse("2027-06-13"), 3, RatePlan.STANDARD));
        service.confirm(june.id());

        String csv = new TourismTaxReport(store).generate(YearMonth.of(2027, 6));

        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-06
                reservation,room,check_in,check_out,guests,nights,tax
                6,201,2027-06-10,2027-06-13,3,3,40.50
                total,40.50
                """, csv);
    }

    @Test
    void ratePlansGroupsByPlan() {
        String csv = new RatePlansReport(store).generate(YearMonth.of(2027, 4));

        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-04
                rate_plan,reservations,amount
                STANDARD,2,1939.60
                NON_REFUNDABLE,0,0.00
                total,2,1939.60
                """, csv);
    }

    @Test
    void ratePlansIncludesAllPlansEvenWhenEmpty() {
        String csv = new RatePlansReport(store).generate(YearMonth.of(2027, 6));

        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-06
                rate_plan,reservations,amount
                STANDARD,0,0.00
                NON_REFUNDABLE,0,0.00
                total,0,0.00
                """, csv);
    }
}
