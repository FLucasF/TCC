package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.rooms.Room;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PriceCalculatorTest {

    private static final Guest GUEST = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");

    private final PriceCalculator calculator = new PriceCalculator();
    private final Room standardRoom = new Rooms().find(101).orElseThrow();
    private final Room deluxeRoom = new Rooms().find(201).orElseThrow();
    private final Room suite = new Rooms().find(301).orElseThrow();

    private Price price(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan) {
        return price(room, checkIn, checkOut, ratePlan, null);
    }

    private Price price(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan ratePlan, String companyCode) {
        return calculator.calculate(
                room, new NewReservation(room.number(), GUEST, checkIn, checkOut, 1, ratePlan, companyCode));
    }

    @Test
    void lowSeasonNightsCostTheNightlyRate() {
        Price price = price(standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 13), RatePlan.STANDARD);

        assertEquals(3, price.nights());
        assertEquals(new BigDecimal("749.70"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("749.70"), price.total());
    }

    @Test
    void highSeasonStartsOnDecember15() {
        Price price = price(standardRoom, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD);

        // December 14: 249.90; December 15: 249.90 + 20% = 299.88
        assertEquals(new BigDecimal("549.78"), price.total());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        Price price = price(standardRoom, LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 2), RatePlan.STANDARD);

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
        Price price = price(standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE);

        // 15% of 249.90 = 37.485, which rounds half-even to 37.48 (half-up would give 37.49)
        assertEquals(new BigDecimal("37.48"), price.discount());
        assertEquals(new BigDecimal("212.42"), price.total());
    }

    @Test
    void nonRefundableDiscountIsOnTheHighSeasonPrice() {
        Price price = price(suite, LocalDate.of(2027, 1, 5), LocalDate.of(2027, 1, 9), RatePlan.NON_REFUNDABLE);

        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), price.subtotal());
        assertEquals(new BigDecimal("417.53"), price.discount());
        assertEquals(new BigDecimal("2365.99"), price.total());
    }

    @Test
    void corporateDiscountDependsOnTheCompany() {
        Price price = price(deluxeRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "TECNOSUL");

        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
    }

    @Test
    void corporateDiscountOfEachCompany() {
        LocalDate in = LocalDate.of(2027, 3, 10);
        LocalDate out = LocalDate.of(2027, 3, 11);

        assertEquals(new BigDecimal("44.98"), price(standardRoom, in, out, RatePlan.CORPORATE, "AGROVALE").discount());
        assertEquals(new BigDecimal("62.48"), price(standardRoom, in, out, RatePlan.CORPORATE, "FARMAVIDA").discount());
    }

    @Test
    void weekendPackageIsTheFixedPriceWithoutDiscount() {
        Price price = price(deluxeRoom, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE);

        assertEquals(2, price.nights());
        assertEquals(new BigDecimal("820.00"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("820.00"), price.total());
    }

    @Test
    void weekendPackageDoesNotChangeInHighSeason() {
        Price price = price(suite, LocalDate.of(2027, 7, 16), LocalDate.of(2027, 7, 18), RatePlan.WEEKEND_PACKAGE);

        assertEquals(new BigDecimal("1200.00"), price.total());
    }

    @Test
    void longStayTakes10PercentOffTheNights() {
        Price price = price(standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), RatePlan.LONG_STAY);

        assertEquals(7, price.nights());
        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }
}
