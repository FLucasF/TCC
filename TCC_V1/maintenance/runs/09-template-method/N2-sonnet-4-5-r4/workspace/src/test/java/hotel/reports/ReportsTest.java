package hotel.reports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.EmptyDatabase;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReportsTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final Rooms rooms = new Rooms(database.dataSource());
    private final ReservationStore store = new ReservationStore(database.dataSource());
    private final ReservationService service = new ReservationService(
            rooms, store, new PriceCalculator(), new EmailSender(new SentMessages(database.dataSource())));

    private Reservation book(int roomNumber, String checkIn, String checkOut, int guests, RatePlan plan) {
        Guest guest = new Guest("Bruno Lima", "bruno@example.com", "987.654.321-00");
        return service.create(new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), guests, plan));
    }


    @AfterEach
    void closeDatabase() {
        database.close();
    }

    @BeforeEach
    void someReservations() {
        Reservation april = book(201, "2027-04-01", "2027-04-03", 1, RatePlan.STANDARD);
        Reservation lateMarch = book(101, "2027-03-30", "2027-04-02", 1, RatePlan.NON_REFUNDABLE);
        Reservation suite = book(301, "2027-04-02", "2027-04-04", 1, RatePlan.STANDARD);
        book(102, "2027-04-01", "2027-04-05", 1, RatePlan.STANDARD); // never paid
        Reservation cancelled = book(202, "2027-04-01", "2027-04-02", 1, RatePlan.STANDARD);
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
        Reservation may = book(101, "2027-05-10", "2027-05-11", 1, RatePlan.NON_REFUNDABLE);
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
    void tourismTaxCountsGuestsAndNights() {
        Reservation twoGuests = book(201, "2027-07-10", "2027-07-13", 2, RatePlan.STANDARD);
        Reservation threeNights = book(101, "2027-07-15", "2027-07-18", 1, RatePlan.NON_REFUNDABLE);
        service.confirm(twoGuests.id());
        service.confirm(threeNights.id());

        String csv = new TourismTaxReport(store).generate(YearMonth.of(2027, 7));

        // 2 guests * 3 nights * 4.50 = 27.00; 1 guest * 3 nights * 4.50 = 13.50
        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-07
                reservation,room,check_in,check_out,guests,nights,tax
                6,201,2027-07-10,2027-07-13,2,3,27.00
                7,101,2027-07-15,2027-07-18,1,3,13.50
                total,40.50
                """, csv);
    }

    @Test
    void tourismTaxCountsOnlyPaidReservations() {
        Reservation confirmed = book(201, "2027-08-01", "2027-08-03", 2, RatePlan.STANDARD);
        book(101, "2027-08-01", "2027-08-02", 1, RatePlan.STANDARD); // pending, not paid
        service.confirm(confirmed.id());

        String csv = new TourismTaxReport(store).generate(YearMonth.of(2027, 8));

        assertEquals("""
                Pousada Maré Alta,tourism-tax,2027-08
                reservation,room,check_in,check_out,guests,nights,tax
                6,201,2027-08-01,2027-08-03,2,2,18.00
                total,18.00
                """, csv);
    }

    @Test
    void ratePlansGroupsRevenueByPlan() {
        Reservation standard1 = book(201, "2027-09-01", "2027-09-03", 1, RatePlan.STANDARD);
        Reservation standard2 = book(101, "2027-09-05", "2027-09-06", 1, RatePlan.STANDARD);
        Reservation nonRefundable = book(301, "2027-09-10", "2027-09-12", 1, RatePlan.NON_REFUNDABLE);
        service.confirm(standard1.id());
        service.confirm(standard2.id());
        service.confirm(nonRefundable.id());

        String csv = new RatePlansReport(store).generate(YearMonth.of(2027, 9));

        // 201 DELUXE: 2 nights * 389.90 = 779.80; 101 STANDARD: 1 night * 249.90 = 249.90
        // 301 SUITE: 2 nights * 579.90 = 1159.80, with 15% discount = 985.83
        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-09
                rate_plan,reservations,amount
                STANDARD,2,1029.70
                NON_REFUNDABLE,1,985.83
                total,3,2015.53
                """, csv);
    }

    @Test
    void ratePlansShowsZeroForUnusedPlans() {
        Reservation onlyStandard = book(201, "2027-10-01", "2027-10-02", 1, RatePlan.STANDARD);
        service.confirm(onlyStandard.id());

        String csv = new RatePlansReport(store).generate(YearMonth.of(2027, 10));

        assertEquals("""
                Pousada Maré Alta,rate-plans,2027-10
                rate_plan,reservations,amount
                STANDARD,1,389.90
                NON_REFUNDABLE,0,0
                total,1,389.90
                """, csv);
    }
}
