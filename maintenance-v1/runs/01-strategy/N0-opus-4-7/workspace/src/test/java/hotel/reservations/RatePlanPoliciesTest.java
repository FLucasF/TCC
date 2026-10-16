package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hotel.messages.EmailSender;
import hotel.messages.SentMessages;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RatePlanPoliciesTest {

    private final ReservationService service = new ReservationService(
            new Rooms(), new ReservationStore(), new RatePlanPolicies(), new EmailSender(new SentMessages()));

    private static NewReservation stay(
            int roomNumber, String checkIn, String checkOut, RatePlan plan, String companyCode) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return new NewReservation(
                roomNumber,
                guest,
                LocalDate.parse(checkIn),
                LocalDate.parse(checkOut),
                1,
                plan,
                companyCode);
    }

    @Test
    void corporatePlanDiscountsPerCompanyAndRefundsEverything() {
        Reservation reservation = service.create(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL"));

        assertEquals(new BigDecimal("779.80"), reservation.price().subtotal());
        assertEquals(new BigDecimal("93.58"), reservation.price().discount());
        assertEquals(new BigDecimal("686.22"), reservation.price().total());

        service.confirm(reservation.id());
        assertEquals(new BigDecimal("686.22"), service.cancel(reservation.id()).refund());
    }

    @Test
    void corporatePlanDemandsAKnownCompanyCode() {
        assertEquals(
                ErrorCode.INVALID_REQUEST,
                assertThrows(
                                HotelException.class,
                                () -> service.create(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, null)))
                        .errorCode());
        assertEquals(
                ErrorCode.INVALID_REQUEST,
                assertThrows(
                                HotelException.class,
                                () -> service.create(stay(201, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "NOPE")))
                        .errorCode());
    }

    @Test
    void weekendPackageIsAFridayToSundayFlatPrice() {
        Reservation reservation = service.create(
                stay(201, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null));

        assertEquals(2, reservation.price().nights());
        assertEquals(new BigDecimal("820.00"), reservation.price().subtotal());
        assertEquals(new BigDecimal("0.00"), reservation.price().discount());
        assertEquals(new BigDecimal("820.00"), reservation.price().total());

        service.confirm(reservation.id());
        assertEquals(new BigDecimal("410.00"), service.cancel(reservation.id()).refund());
    }

    @Test
    void weekendPackageOnlyRunsFridayToSunday() {
        // Saturday to Monday
        assertEquals(
                ErrorCode.INVALID_REQUEST,
                assertThrows(
                                HotelException.class,
                                () -> service.create(
                                        stay(201, "2027-03-13", "2027-03-15", RatePlan.WEEKEND_PACKAGE, null)))
                        .errorCode());
        // Friday to the Sunday a week later (nine nights)
        assertEquals(
                ErrorCode.INVALID_REQUEST,
                assertThrows(
                                HotelException.class,
                                () -> service.create(
                                        stay(201, "2027-03-12", "2027-03-21", RatePlan.WEEKEND_PACKAGE, null)))
                        .errorCode());
    }

    @Test
    void longStayDiscountsNightlySubtotalByTenPercent() {
        Reservation reservation = service.create(stay(101, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY, null));

        assertEquals(7, reservation.price().nights());
        assertEquals(new BigDecimal("1749.30"), reservation.price().subtotal());
        assertEquals(new BigDecimal("174.93"), reservation.price().discount());
        assertEquals(new BigDecimal("1574.37"), reservation.price().total());

        service.confirm(reservation.id());
        assertEquals(new BigDecimal("1574.37"), service.cancel(reservation.id()).refund());
    }

    @Test
    void longStayNeedsAtLeastSevenNights() {
        assertEquals(
                ErrorCode.INVALID_REQUEST,
                assertThrows(
                                HotelException.class,
                                () -> service.create(stay(101, "2027-03-01", "2027-03-07", RatePlan.LONG_STAY, null)))
                        .errorCode());
    }
}
