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
        return service.create(
                new NewReservation(room, guest, LocalDate.parse(in), LocalDate.parse(out), 2, plan, code));
    }

    private ErrorCode errorOf(int room, String in, String out, RatePlan plan, String code) {
        return assertThrows(HotelException.class, () -> book(room, in, out, plan, code)).errorCode();
    }

    @Test
    void corporateDiscountDependsOnTheCompany() {
        Price price = book(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL").price();
        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
        assertEquals(new BigDecimal("0.18").multiply(new BigDecimal("779.80")).setScale(2, java.math.RoundingMode.HALF_EVEN),
                book(202, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "AGROVALE").price().discount());
    }

    @Test
    void corporateNeedsAKnownCompany() {
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, null));
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "ACME"));
    }

    @Test
    void companyCodeIsIgnoredByOtherPlans() {
        Price price = book(101, "2027-03-10", "2027-03-11", RatePlan.STANDARD, "ACME").price();
        assertEquals(new BigDecimal("249.90"), price.total());
    }

    @Test
    void weekendPackageHasAClosedPriceEvenInHighSeason() {
        Price price = book(201, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null).price();
        assertEquals(new BigDecimal("820.00"), price.total());
        assertEquals(new BigDecimal("820.00"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("1200.00"),
                book(301, "2027-07-09", "2027-07-11", RatePlan.WEEKEND_PACKAGE, null).price().total());
    }

    @Test
    void weekendPackageOnlyFridayToSunday() {
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(101, "2027-03-13", "2027-03-15", RatePlan.WEEKEND_PACKAGE, null));
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(101, "2027-03-12", "2027-03-13", RatePlan.WEEKEND_PACKAGE, null));
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(101, "2027-03-12", "2027-03-15", RatePlan.WEEKEND_PACKAGE, null));
    }

    @Test
    void weekendPackageRefundsHalf() {
        Reservation reservation = book(201, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null);
        service.confirm(reservation.id());
        assertEquals(new BigDecimal("410.00"), service.cancel(reservation.id()).refund());
    }

    @Test
    void longStayDiscountAndMinimumNights() {
        Price price = book(101, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY, null).price();
        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(102, "2027-03-01", "2027-03-07", RatePlan.LONG_STAY, null));
    }

    @Test
    void corporateAndLongStayRefundEverything() {
        Reservation corporate = book(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "FARMAVIDA");
        service.confirm(corporate.id());
        assertEquals(corporate.price().total(), service.cancel(corporate.id()).refund());
        Reservation longStay = book(101, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY, null);
        service.confirm(longStay.id());
        assertEquals(longStay.price().total(), service.cancel(longStay.id()).refund());
    }
}
