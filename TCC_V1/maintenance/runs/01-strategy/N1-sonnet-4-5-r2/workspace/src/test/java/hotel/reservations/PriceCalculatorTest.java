package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PriceCalculatorTest {

    private final PriceCalculator calculator = new PriceCalculator();
    private final Room standardRoom = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
    private final Room suite = new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90"));

    @Test
    void lowSeasonNightsCostTheNightlyRate() {
        NewReservation request = stay(101, "2027-03-10", "2027-03-13", RatePlan.STANDARD);
        Price price = calculator.calculate(
                standardRoom, request.checkIn(), request.checkOut(), request);

        assertEquals(3, price.nights());
        assertEquals(new BigDecimal("749.70"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("749.70"), price.total());
    }

    private static NewReservation stay(int roomNumber, String checkIn, String checkOut, RatePlan plan) {
        Guest guest = new Guest("Ana", "ana@example.com", "123.456.789-09");
        return new NewReservation(
                roomNumber, guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 2, plan, null);
    }

    @Test
    void highSeasonStartsOnDecember15() {
        NewReservation request = stay(101, "2027-12-14", "2027-12-16", RatePlan.STANDARD);
        Price price = calculator.calculate(
                standardRoom, request.checkIn(), request.checkOut(), request);

        // December 14: 249.90; December 15: 249.90 + 20% = 299.88
        assertEquals(new BigDecimal("549.78"), price.total());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        NewReservation request = stay(101, "2028-02-29", "2028-03-02", RatePlan.STANDARD);
        Price price = calculator.calculate(
                standardRoom, request.checkIn(), request.checkOut(), request);

        // February 29 (leap year): 299.88; March 1: 249.90
        assertEquals(new BigDecimal("549.78"), price.total());
    }

    @Test
    void julyIsHighSeason() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 6, 30)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void nonRefundableTakes15PercentOffRoundingHalfEven() {
        NewReservation request = stay(101, "2027-03-10", "2027-03-11", RatePlan.NON_REFUNDABLE);
        Price price = calculator.calculate(
                standardRoom, request.checkIn(), request.checkOut(), request);

        // 15% of 249.90 = 37.485, which rounds half-even to 37.48 (half-up would give 37.49)
        assertEquals(new BigDecimal("37.48"), price.discount());
        assertEquals(new BigDecimal("212.42"), price.total());
    }

    @Test
    void nonRefundableDiscountIsOnTheHighSeasonPrice() {
        NewReservation request = stay(301, "2027-01-05", "2027-01-09", RatePlan.NON_REFUNDABLE);
        Price price = calculator.calculate(
                suite, request.checkIn(), request.checkOut(), request);

        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), price.subtotal());
        assertEquals(new BigDecimal("417.53"), price.discount());
        assertEquals(new BigDecimal("2365.99"), price.total());
    }

    @Test
    void corporatePlanAppliesCompanyDiscount() {
        Room deluxeRoom = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));
        Guest guest = new Guest("Ana", "ana@example.com", "123.456.789-09");
        NewReservation request = new NewReservation(
                201, guest, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.CORPORATE, "TECNOSUL");

        Price price = calculator.calculate(deluxeRoom, request.checkIn(), request.checkOut(), request);

        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
    }

    @Test
    void weekendPackageUsesFixedPrice() {
        Room deluxeRoom = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));
        Guest guest = new Guest("Ana", "ana@example.com", "123.456.789-09");
        NewReservation request = new NewReservation(
                201, guest, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), 2, RatePlan.WEEKEND_PACKAGE, null);

        Price price = calculator.calculate(deluxeRoom, request.checkIn(), request.checkOut(), request);

        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("820.00"), price.total());
    }

    @Test
    void longStayGives10PercentOff() {
        NewReservation request = new NewReservation(
                101, new Guest("Ana", "ana@example.com", "123.456.789-09"),
                LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), 2, RatePlan.LONG_STAY, null);

        Price price = calculator.calculate(standardRoom, request.checkIn(), request.checkOut(), request);

        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }
}
