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
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return new NewReservation(room, guest, LocalDate.parse(in), LocalDate.parse(out), 2, plan, company);
    }

    private void assertInvalid(NewReservation request) {
        assertEquals(ErrorCode.INVALID_REQUEST,
                assertThrows(HotelException.class, () -> service.create(request)).errorCode());
    }

    @Test
    void corporateDiscountsByCompany() {
        Reservation r = service.create(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL"));
        assertEquals(new BigDecimal("779.80"), r.price().subtotal());
        assertEquals(new BigDecimal("93.58"), r.price().discount());
        assertEquals(new BigDecimal("686.22"), r.price().total());
        assertEquals("TECNOSUL", service.find(r.id()).companyCode());

        Reservation a = service.create(stay(202, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "AGROVALE"));
        assertEquals(new BigDecimal("140.36"), a.price().discount());
        Reservation f = service.create(stay(101, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "FARMAVIDA"));
        assertEquals(new BigDecimal("124.95"), f.price().discount());
    }

    @Test
    void corporateNeedsAKnownCompanyAndRefundsEverything() {
        assertInvalid(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, null));
        assertInvalid(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "OUTRA"));
        Reservation r = service.create(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL"));
        service.confirm(r.id());
        assertEquals(new BigDecimal("686.22"), service.cancel(r.id()).refund());
    }

    @Test
    void weekendPackageHasAFixedPriceAndRefundsHalf() {
        Reservation r = service.create(stay(201, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null));
        assertEquals(2, r.price().nights());
        assertEquals(new BigDecimal("820.00"), r.price().subtotal());
        assertEquals(new BigDecimal("0.00"), r.price().discount());
        assertEquals(new BigDecimal("820.00"), r.price().total());
        service.confirm(r.id());
        assertEquals(new BigDecimal("410.00"), service.cancel(r.id()).refund());

        // high season, same fixed price
        Reservation s = service.create(stay(301, "2027-01-08", "2027-01-10", RatePlan.WEEKEND_PACKAGE, null));
        assertEquals(new BigDecimal("1200.00"), s.price().total());
        Reservation st = service.create(stay(101, "2027-07-09", "2027-07-11", RatePlan.WEEKEND_PACKAGE, null));
        assertEquals(new BigDecimal("520.00"), st.price().total());
    }

    @Test
    void weekendPackageOnlyFridayToSunday() {
        assertInvalid(stay(201, "2027-03-11", "2027-03-13", RatePlan.WEEKEND_PACKAGE, null));
        assertInvalid(stay(201, "2027-03-12", "2027-03-15", RatePlan.WEEKEND_PACKAGE, null));
        assertInvalid(stay(201, "2027-03-12", "2027-03-13", RatePlan.WEEKEND_PACKAGE, null));
    }

    @Test
    void longStayNeedsSevenNightsAndRefundsEverything() {
        Reservation r = service.create(stay(101, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY, null));
        assertEquals(new BigDecimal("1749.30"), r.price().subtotal());
        assertEquals(new BigDecimal("174.93"), r.price().discount());
        assertEquals(new BigDecimal("1574.37"), r.price().total());
        service.confirm(r.id());
        assertEquals(new BigDecimal("1574.37"), service.cancel(r.id()).refund());
        assertInvalid(stay(101, "2027-03-01", "2027-03-07", RatePlan.LONG_STAY, null));
    }

    @Test
    void longStayKeepsHighSeason() {
        Reservation r = service.create(stay(101, "2027-02-24", "2027-03-03", RatePlan.LONG_STAY, null));
        // 5 nights (Feb 24-28) at 299.88 + 2 at 249.90 = 1999.20; 10% = 199.92
        assertEquals(new BigDecimal("1999.20"), r.price().subtotal());
        assertEquals(new BigDecimal("199.92"), r.price().discount());
    }
}
