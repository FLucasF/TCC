package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hotel.rooms.Room;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PriceCalculatorTest {

    private final PriceCalculator calculator = new PriceCalculator();
    private final Room standardRoom = new Rooms().find(101).orElseThrow();
    private final Room deluxe = new Rooms().find(201).orElseThrow();
    private final Room suite = new Rooms().find(301).orElseThrow();

    @Test
    void lowSeasonNightsCostTheNightlyRate() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 13), RatePlan.STANDARD);

        assertEquals(3, price.nights());
        assertEquals(new BigDecimal("749.70"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("749.70"), price.total());
    }

    @Test
    void highSeasonStartsOnDecember15() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD);

        // December 14: 249.90; December 15: 249.90 + 20% = 299.88
        assertEquals(new BigDecimal("549.78"), price.total());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 2), RatePlan.STANDARD);

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
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE);

        // 15% of 249.90 = 37.485, which rounds half-even to 37.48 (half-up would give 37.49)
        assertEquals(new BigDecimal("37.48"), price.discount());
        assertEquals(new BigDecimal("212.42"), price.total());
    }

    @Test
    void nonRefundableDiscountIsOnTheHighSeasonPrice() {
        Price price = calculator.calculate(
                suite, LocalDate.of(2027, 1, 5), LocalDate.of(2027, 1, 9), RatePlan.NON_REFUNDABLE);

        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), price.subtotal());
        assertEquals(new BigDecimal("417.53"), price.discount());
        assertEquals(new BigDecimal("2365.99"), price.total());
    }

    @Test
    void corporateDiscountDependsOnTheCompanyCode() {
        Price price = calculator.calculate(
                deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "TECNOSUL");

        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
    }

    @Test
    void corporateNeedsAKnownCompanyCode() {
        assertThrows(HotelException.class, () -> calculator.calculate(
                deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, null));
        assertThrows(HotelException.class, () -> calculator.calculate(
                deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "UNKNOWN"));
    }

    @Test
    void weekendPackageIsAFlatPriceForFridayToSunday() {
        Price price = calculator.calculate(
                deluxe, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE);

        assertEquals(2, price.nights());
        assertEquals(new BigDecimal("820.00"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("820.00"), price.total());
    }

    @Test
    void weekendPackageOnlyWorksFridayToSunday() {
        assertThrows(HotelException.class, () -> calculator.calculate(
                deluxe, LocalDate.of(2027, 3, 11), LocalDate.of(2027, 3, 13), RatePlan.WEEKEND_PACKAGE));
        assertThrows(HotelException.class, () -> calculator.calculate(
                deluxe, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 15), RatePlan.WEEKEND_PACKAGE));
    }

    @Test
    void longStayTakes10PercentOffAfter7Nights() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), RatePlan.LONG_STAY);

        assertEquals(7, price.nights());
        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void longStayNeedsAtLeast7Nights() {
        assertThrows(HotelException.class, () -> calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 7), RatePlan.LONG_STAY));
    }
}
