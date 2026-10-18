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

    private final PriceCalculator calculator = new PriceCalculator();
    private final Room standardRoom = new Rooms().find(101).orElseThrow();
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

        // December 14: 249.90; December 15: 249.90 + 25% = 312.38
        assertEquals(new BigDecimal("562.28"), price.total());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 2), RatePlan.STANDARD);

        // February 29 (leap year): 312.38; March 1: 249.90
        assertEquals(new BigDecimal("562.28"), price.total());
    }

    @Test
    void julyIsHighSeason() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 6, 30)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void easterWeekIsHighSeason() {
        // Easter 2027 is March 28, so Easter week is March 21 (Sunday of Ramps) to March 28 (Sunday of Easter)
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 20))); // Saturday before
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 21))); // Sunday of Ramps
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 27))); // Saturday during
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 28))); // Easter Sunday
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 29))); // Monday after Easter
    }

    @Test
    void easterWeekPricing2027() {
        // March 20 (low season): 249.90; March 21 (Easter week): 312.38; March 22 (Easter week): 312.38; March 23 (Easter week): 312.38
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 20), LocalDate.of(2027, 3, 24), RatePlan.STANDARD);
        // 249.90 + 312.38 + 312.38 + 312.38 = 1187.04
        assertEquals(new BigDecimal("1187.04"), price.total());
    }

    @Test
    void easterWeekPricing2028() {
        // Easter 2028 is April 16, so Easter week is April 9 (Sunday of Ramps) to April 16 (Sunday of Easter)
        // April 8 (low season): 249.90; April 9 (Easter week): 312.38; April 10 (Easter week): 312.38; April 11 (Easter week): 312.38
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2028, 4, 8), LocalDate.of(2028, 4, 12), RatePlan.STANDARD);
        // 249.90 + 312.38 + 312.38 + 312.38 = 1187.04
        assertEquals(new BigDecimal("1187.04"), price.total());
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

        // 4 nights of 579.90 + 25% = 724.88 each
        assertEquals(new BigDecimal("2899.52"), price.subtotal());
        assertEquals(new BigDecimal("434.93"), price.discount());
        assertEquals(new BigDecimal("2464.59"), price.total());
    }
}
