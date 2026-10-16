package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RatePlansTest {

    private final ReservationService service = new ReservationService(
            new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(new SentMessages()));

    private Reservation book(int room, String in, String out, RatePlan plan, String code) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return service.create(new NewReservation(
                room, guest, LocalDate.parse(in), LocalDate.parse(out), 2, plan, code));
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(new BigDecimal(expected), actual);
    }

    @Test
    void corporateDiscountsByCompany() {
        Price price = book(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL").price();
        assertMoney("779.80", price.subtotal());
        assertMoney("93.58", price.discount());
        assertMoney("686.22", price.total());
    }

    @Test
    void corporateNeedsAKnownCode() {
        for (String code : new String[] {null, "", "ACME"}) {
            HotelException e = assertThrows(HotelException.class,
                    () -> book(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, code));
            assertEquals(ErrorCode.INVALID_REQUEST, e.errorCode());
        }
    }

    @Test
    void companyCodeIsIgnoredByOtherPlans() {
        Price price = book(201, "2027-03-10", "2027-03-12", RatePlan.STANDARD, "ACME").price();
        assertMoney("0.00", price.discount());
    }

    @Test
    void weekendPackageHasAClosedPriceAndHalfRefund() {
        Reservation r = book(201, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null);
        assertMoney("820.00", r.price().subtotal());
        assertMoney("0.00", r.price().discount());
        assertMoney("820.00", r.price().total());
        service.confirm(r.id());
        assertMoney("410.00", service.cancel(r.id()).refund());
    }

    @Test
    void weekendPackageOnlyFridayToSunday() {
        for (String[] dates : new String[][] {
            {"2027-03-13", "2027-03-15"}, {"2027-03-12", "2027-03-15"}, {"2027-03-12", "2027-03-13"}}) {
            HotelException e = assertThrows(HotelException.class,
                    () -> book(201, dates[0], dates[1], RatePlan.WEEKEND_PACKAGE, null));
            assertEquals(ErrorCode.INVALID_REQUEST, e.errorCode());
        }
    }

    @Test
    void longStayDiscountsAndRefundsEverything() {
        Reservation r = book(101, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY, null);
        assertMoney("1749.30", r.price().subtotal());
        assertMoney("174.93", r.price().discount());
        assertMoney("1574.37", r.price().total());
        service.confirm(r.id());
        assertMoney("1574.37", service.cancel(r.id()).refund());
    }

    @Test
    void longStayNeedsSevenNights() {
        HotelException e = assertThrows(HotelException.class,
                () -> book(101, "2027-03-01", "2027-03-07", RatePlan.LONG_STAY, null));
        assertEquals(ErrorCode.INVALID_REQUEST, e.errorCode());
    }
}
