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

    private Reservation book(int room, String in, String out, RatePlan plan, String companyCode) {
        Guest guest = new Guest("Carla Dias", "carla@example.com", "111.222.333-44");
        return service.create(new NewReservation(
                room, guest, LocalDate.parse(in), LocalDate.parse(out), 1, plan, companyCode));
    }

    private ErrorCode errorOf(Runnable action) {
        return assertThrows(HotelException.class, action::run).errorCode();
    }

    @Test
    void corporateGivesTheCompanyDiscount() {
        Reservation reservation = book(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL");

        assertEquals(new BigDecimal("779.80"), reservation.price().subtotal());
        assertEquals(new BigDecimal("93.58"), reservation.price().discount());
        assertEquals(new BigDecimal("686.22"), reservation.price().total());
    }

    @Test
    void corporateDiscountsAreOneForEachCompany() {
        assertEquals(new BigDecimal("140.36"),
                book(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "AGROVALE").price().discount());
        assertEquals(new BigDecimal("194.95"),
                book(202, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "FARMAVIDA").price().discount());
    }

    @Test
    void corporateNeedsAKnownCompanyCode() {
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> book(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, null)));
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> book(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "tecnosul")));
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> book(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "OUTRA")));
    }

    @Test
    void otherPlansIgnoreTheCompanyCode() {
        Reservation reservation = book(201, "2027-03-10", "2027-03-12", RatePlan.STANDARD, "TECNOSUL");

        assertEquals(new BigDecimal("779.80"), reservation.price().total());
    }

    @Test
    void corporateCancellationGivesEverythingBack() {
        Reservation reservation = book(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL");
        service.confirm(reservation.id());

        assertEquals(new BigDecimal("686.22"), service.cancel(reservation.id()).refund());
    }

    @Test
    void weekendPackageHasAClosedPrice() {
        Reservation reservation = book(201, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null);

        assertEquals(2, reservation.price().nights());
        assertEquals(new BigDecimal("820.00"), reservation.price().subtotal());
        assertEquals(new BigDecimal("0.00"), reservation.price().discount());
        assertEquals(new BigDecimal("820.00"), reservation.price().total());
    }

    @Test
    void weekendPackageHasTheSamePriceInHighSeasonAndForEveryRoomType() {
        // 2027-07-09 is a Friday
        assertEquals(new BigDecimal("520.00"),
                book(101, "2027-07-09", "2027-07-11", RatePlan.WEEKEND_PACKAGE, null).price().total());
        assertEquals(new BigDecimal("1200.00"),
                book(301, "2027-07-09", "2027-07-11", RatePlan.WEEKEND_PACKAGE, null).price().total());
    }

    @Test
    void weekendPackageOnlyFromFridayToSunday() {
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> book(201, "2027-03-13", "2027-03-15", RatePlan.WEEKEND_PACKAGE, null)));
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> book(201, "2027-03-12", "2027-03-15", RatePlan.WEEKEND_PACKAGE, null)));
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> book(201, "2027-03-12", "2027-03-13", RatePlan.WEEKEND_PACKAGE, null)));
    }

    @Test
    void weekendPackageCancellationGivesHalfBack() {
        Reservation reservation = book(201, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null);
        service.confirm(reservation.id());

        Reservation cancelled = service.cancel(reservation.id());

        assertEquals(new BigDecimal("410.00"), cancelled.refund());
        assertEquals(new BigDecimal("410.00"), cancelled.amountKept());
    }

    @Test
    void longStayGivesTenPercentOff() {
        Reservation reservation = book(101, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY, null);

        assertEquals(7, reservation.price().nights());
        assertEquals(new BigDecimal("1749.30"), reservation.price().subtotal());
        assertEquals(new BigDecimal("174.93"), reservation.price().discount());
        assertEquals(new BigDecimal("1574.37"), reservation.price().total());
    }

    @Test
    void longStayKeepsTheHighSeason() {
        // 2027-06-28 to 2027-07-05: 3 nights in June, 4 in July (+20%)
        Reservation reservation = book(101, "2027-06-28", "2027-07-05", RatePlan.LONG_STAY, null);

        assertEquals(new BigDecimal("1949.22"), reservation.price().subtotal());
        assertEquals(new BigDecimal("194.92"), reservation.price().discount());
        assertEquals(new BigDecimal("1754.30"), reservation.price().total());
    }

    @Test
    void longStayNeedsSevenNights() {
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> book(101, "2027-03-01", "2027-03-07", RatePlan.LONG_STAY, null)));
    }

    @Test
    void longStayCancellationGivesEverythingBack() {
        Reservation reservation = book(101, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY, null);
        service.confirm(reservation.id());

        assertEquals(new BigDecimal("1574.37"), service.cancel(reservation.id()).refund());
    }
}
