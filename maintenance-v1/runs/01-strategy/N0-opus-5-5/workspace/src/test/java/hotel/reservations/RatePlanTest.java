package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RatePlanTest {

    private final ReservationService service = new ReservationService(
            new Rooms(), new ReservationStore(), new PriceCalculator(), new EmailSender(new SentMessages()));

    private static NewReservation stay(
            int roomNumber, String checkIn, String checkOut, RatePlan plan, String companyCode) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 1, plan, companyCode);
    }

    private static ErrorCode errorOf(Runnable action) {
        return assertThrows(HotelException.class, action::run).errorCode();
    }

    private BigDecimal refundAfterPaying(Reservation reservation) {
        service.confirm(reservation.id());
        return service.cancel(reservation.id()).refund();
    }

    @Test
    void corporateGivesTheCompanyDiscount() {
        Reservation tecnosul =
                service.create(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL"));

        assertEquals(new BigDecimal("779.80"), tecnosul.price().subtotal());
        assertEquals(new BigDecimal("93.58"), tecnosul.price().discount());
        assertEquals(new BigDecimal("686.22"), tecnosul.price().total());
        assertEquals("TECNOSUL", tecnosul.companyCode());

        Reservation agrovale = service.create(stay(101, "2027-03-10", "2027-03-11", RatePlan.CORPORATE, "AGROVALE"));
        Reservation farmavida = service.create(stay(301, "2027-03-10", "2027-03-11", RatePlan.CORPORATE, "FARMAVIDA"));
        assertEquals(new BigDecimal("44.98"), agrovale.price().discount()); // 18% of 249.90 = 44.982
        assertEquals(new BigDecimal("144.98"), farmavida.price().discount()); // 25% of 579.90 = 144.975
    }

    @Test
    void corporateNeedsTheCodeOfACompanyWithAnAgreement() {
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> service.create(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, null))));
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> service.create(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "  "))));
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> service.create(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "ACME"))));
    }

    @Test
    void corporateRefundsEverything() {
        Reservation reservation =
                service.create(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL"));

        assertEquals(new BigDecimal("686.22"), refundAfterPaying(reservation));
    }

    @Test
    void theCompanyCodeIsIgnoredByTheOtherPlans() {
        Reservation reservation = service.create(stay(101, "2027-03-10", "2027-03-11", RatePlan.STANDARD, "ACME"));

        assertNull(reservation.companyCode());
        assertEquals(new BigDecimal("249.90"), reservation.price().total());
    }

    @Test
    void theWeekendPackageHasAFixedPricePerRoomType() {
        Reservation deluxe =
                service.create(stay(201, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null));

        assertEquals(2, deluxe.price().nights());
        assertEquals(new BigDecimal("820.00"), deluxe.price().subtotal());
        assertEquals(new BigDecimal("0.00"), deluxe.price().discount());
        assertEquals(new BigDecimal("820.00"), deluxe.price().total());

        // the same price in high season
        assertEquals(new BigDecimal("520.00"),
                service.create(stay(101, "2027-07-09", "2027-07-11", RatePlan.WEEKEND_PACKAGE, null)).price().total());
        assertEquals(new BigDecimal("1200.00"),
                service.create(stay(301, "2027-07-09", "2027-07-11", RatePlan.WEEKEND_PACKAGE, null)).price().total());
    }

    @Test
    void theWeekendPackageIsOnlyFromFridayToSunday() {
        // Thursday to Saturday, Saturday to Monday, Friday to Saturday, Friday to the next Sunday
        for (String[] dates : new String[][] {
                {"2027-03-11", "2027-03-13"}, {"2027-03-13", "2027-03-15"},
                {"2027-03-12", "2027-03-13"}, {"2027-03-12", "2027-03-21"}}) {
            assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() ->
                    service.create(stay(201, dates[0], dates[1], RatePlan.WEEKEND_PACKAGE, null))));
        }
    }

    @Test
    void theWeekendPackageRefundsHalf() {
        Reservation reservation =
                service.create(stay(201, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null));

        assertEquals(new BigDecimal("410.00"), refundAfterPaying(reservation));
        assertEquals(new BigDecimal("410.00"), reservation.amountKept());
    }

    @Test
    void aPendingWeekendPackageRefundsNothing() {
        Reservation reservation =
                service.create(stay(201, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null));

        assertEquals(new BigDecimal("0.00"), service.cancel(reservation.id()).refund());
    }

    @Test
    void aLongStayGets10PercentOff() {
        Reservation reservation = service.create(stay(101, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY, null));

        assertEquals(7, reservation.price().nights());
        assertEquals(new BigDecimal("1749.30"), reservation.price().subtotal());
        assertEquals(new BigDecimal("174.93"), reservation.price().discount());
        assertEquals(new BigDecimal("1574.37"), reservation.price().total());
    }

    @Test
    void aLongStayKeepsTheHighSeason() {
        Reservation reservation = service.create(stay(101, "2027-02-25", "2027-03-04", RatePlan.LONG_STAY, null));

        // February 25 to 28: 4 x 299.88; March 1 to 3: 3 x 249.90
        assertEquals(new BigDecimal("1949.22"), reservation.price().subtotal());
        assertEquals(new BigDecimal("194.92"), reservation.price().discount());
        assertEquals(new BigDecimal("1754.30"), reservation.price().total());
    }

    @Test
    void aLongStayNeedsSevenNights() {
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> service.create(stay(101, "2027-03-01", "2027-03-07", RatePlan.LONG_STAY, null))));
    }

    @Test
    void aLongStayRefundsEverything() {
        Reservation reservation = service.create(stay(101, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY, null));

        assertEquals(new BigDecimal("1574.37"), refundAfterPaying(reservation));
    }
}
