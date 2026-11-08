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

    private static NewReservation request(LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return new NewReservation(101, guest, checkIn, checkOut, 1, plan, null);
    }

    @Test
    void lowSeasonNightsCostTheNightlyRate() {
        Price price = calculator.calculate(standardRoom, request(LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 13), RatePlan.STANDARD));

        assertEquals(3, price.nights());
        assertEquals(new BigDecimal("749.70"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("749.70"), price.total());
    }

    @Test
    void highSeasonStartsOnDecember15() {
        Price price = calculator.calculate(standardRoom, request(LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD));

        // December 14: 249.90; December 15: 249.90 + 20% = 299.88
        assertEquals(new BigDecimal("549.78"), price.total());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        Price price = calculator.calculate(standardRoom, request(LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 2), RatePlan.STANDARD));

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
        Price price = calculator.calculate(standardRoom, request(LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE));

        // 15% of 249.90 = 37.485, which rounds half-even to 37.48 (half-up would give 37.49)
        assertEquals(new BigDecimal("37.48"), price.discount());
        assertEquals(new BigDecimal("212.42"), price.total());
    }

    @Test
    void nonRefundableDiscountIsOnTheHighSeasonPrice() {
        Price price = calculator.calculate(suite, request(LocalDate.of(2027, 1, 5), LocalDate.of(2027, 1, 9), RatePlan.NON_REFUNDABLE));

        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), price.subtotal());
        assertEquals(new BigDecimal("417.53"), price.discount());
        assertEquals(new BigDecimal("2365.99"), price.total());
    }

    private final Room deluxe = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));

    @Test
    void corporateDiscountDependsOnTheCompany() {
        NewReservation tecnosul = new NewReservation(
                201, new Guest("Ana Souza", "ana@example.com", "123.456.789-09"),
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 1, RatePlan.CORPORATE, "TECNOSUL");

        Price price = calculator.calculate(deluxe, tecnosul);

        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
    }

    @Test
    void weekendPackageIsTheFixedPriceEvenInHighSeason() {
        // Friday, January 8, 2027
        Price price = calculator.calculate(
                suite, request(LocalDate.of(2027, 1, 8), LocalDate.of(2027, 1, 10), RatePlan.WEEKEND_PACKAGE));

        assertEquals(2, price.nights());
        assertEquals(new BigDecimal("1200.00"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("1200.00"), price.total());
        assertEquals(new BigDecimal("820.00"), calculator.calculate(deluxe,
                request(LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE)).total());
        assertEquals(new BigDecimal("520.00"), calculator.calculate(standardRoom,
                request(LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE)).total());
    }

    @Test
    void longStayTakes10PercentOff() {
        Price price = calculator.calculate(
                standardRoom, request(LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), RatePlan.LONG_STAY));

        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void longStayKeepsTheHighSeasonOnItsNights() {
        Price price = calculator.calculate(
                standardRoom, request(LocalDate.of(2027, 12, 12), LocalDate.of(2027, 12, 19), RatePlan.LONG_STAY));

        // 3 nights of 249.90 + 4 nights of 299.88
        assertEquals(new BigDecimal("1949.22"), price.subtotal());
    }
}
