package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hotel.EmptyDatabase;
import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RatePlansTest {

    private final EmptyDatabase database = new EmptyDatabase();
    private final ReservationService service = new ReservationService(
            new Rooms(database.dataSource()), new ReservationStore(database.dataSource()), new PriceCalculator(),
            new EmailSender(new SentMessages(database.dataSource())));

    @AfterEach
    void closeDatabase() {
        database.close();
    }

    private static NewReservation stay(int room, String in, String out, RatePlan plan, String company) {
        return new NewReservation(room, new Guest("Ana Souza", "ana@example.com", "123.456.789-09"),
                LocalDate.parse(in), LocalDate.parse(out), 2, plan, company);
    }

    private void assertInvalid(NewReservation request) {
        assertEquals(ErrorCode.INVALID_REQUEST, assertThrows(HotelException.class, () -> service.create(request)).errorCode());
    }

    @Test
    void corporatePlanDiscountsByCompany() {
        Price price = service.create(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL")).price();

        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
        assertEquals(new BigDecimal("187.42"),
                service.create(stay(101, "2027-03-10", "2027-03-13", RatePlan.CORPORATE, "FARMAVIDA")).price().discount());
    }

    @Test
    void corporatePlanNeedsAKnownCompany() {
        assertInvalid(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, null));
        assertInvalid(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "OUTRA"));
        assertInvalid(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "tecnosul"));
    }

    @Test
    void corporateCancellationRefundsEverything() {
        Reservation r = service.create(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "AGROVALE"));
        service.confirm(r.id());
        assertEquals(new BigDecimal("639.44"), service.cancel(r.id()).refund());
    }

    @Test
    void weekendPackageHasAClosedPriceAndHalfRefund() {
        Reservation r = service.create(stay(201, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null));
        assertEquals(new BigDecimal("820.00"), r.price().subtotal());
        assertEquals(new BigDecimal("0.00"), r.price().discount());
        assertEquals(new BigDecimal("820.00"), r.price().total());
        service.confirm(r.id());
        assertEquals(new BigDecimal("410.00"), service.cancel(r.id()).refund());

        // in high season the price is the same
        assertEquals(new BigDecimal("1200.00"),
                service.create(stay(301, "2027-07-09", "2027-07-11", RatePlan.WEEKEND_PACKAGE, null)).price().total());
        assertEquals(new BigDecimal("520.00"),
                service.create(stay(101, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null)).price().total());
    }

    @Test
    void weekendPackageOnlyFridayToSunday() {
        assertInvalid(stay(201, "2027-03-11", "2027-03-13", RatePlan.WEEKEND_PACKAGE, null));
        assertInvalid(stay(201, "2027-03-12", "2027-03-15", RatePlan.WEEKEND_PACKAGE, null));
        assertInvalid(stay(201, "2027-03-12", "2027-03-13", RatePlan.WEEKEND_PACKAGE, null));
    }

    @Test
    void longStayNeedsSevenNightsAndGivesTenPercent() {
        Reservation r = service.create(stay(101, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY, null));
        assertEquals(new BigDecimal("1749.30"), r.price().subtotal());
        assertEquals(new BigDecimal("174.93"), r.price().discount());
        assertEquals(new BigDecimal("1574.37"), r.price().total());
        service.confirm(r.id());
        assertEquals(new BigDecimal("1574.37"), service.cancel(r.id()).refund());

        assertInvalid(stay(102, "2027-03-01", "2027-03-07", RatePlan.LONG_STAY, null));
    }

    @Test
    void longStayKeepsHighSeason() {
        // 7 nights from Feb 26 to Mar 5: Feb 26-28 (3 nights) at 299.88, 4 nights at 249.90
        Price price = service.create(stay(101, "2027-02-26", "2027-03-05", RatePlan.LONG_STAY, null)).price();
        assertEquals(new BigDecimal("1899.24"), price.subtotal());
    }
}
