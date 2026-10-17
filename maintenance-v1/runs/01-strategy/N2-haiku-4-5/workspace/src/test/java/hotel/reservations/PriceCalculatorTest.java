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
    private final RatePlanStrategyFactory strategyFactory = new RatePlanStrategyFactory(calculator);
    private final Room standardRoom = new Rooms().find(101).orElseThrow();
    private final Room suite = new Rooms().find(301).orElseThrow();

    @Test
    void lowSeasonNightsCostTheNightlyRate() {
        RatePlanStrategy strategy = strategyFactory.createStrategy(RatePlan.STANDARD, null);
        Price price = strategy.calculate(standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 13));

        assertEquals(3, price.nights());
        assertEquals(new BigDecimal("749.70"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("749.70"), price.total());
    }

    @Test
    void highSeasonStartsOnDecember15() {
        RatePlanStrategy strategy = strategyFactory.createStrategy(RatePlan.STANDARD, null);
        Price price = strategy.calculate(
                standardRoom, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16));

        // December 14: 249.90; December 15: 249.90 + 20% = 299.88
        assertEquals(new BigDecimal("549.78"), price.total());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        RatePlanStrategy strategy = strategyFactory.createStrategy(RatePlan.STANDARD, null);
        Price price = strategy.calculate(
                standardRoom, LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 2));

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
    void corporateTecnosulGets12PercentDiscount() {
        RatePlanStrategy strategy = strategyFactory.createStrategy(RatePlan.CORPORATE, "TECNOSUL");
        Price price = strategy.calculate(
                new Rooms().find(202).orElseThrow(),
                LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12));

        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
    }

    @Test
    void weekendPackageDeluxe() {
        RatePlanStrategy strategy = strategyFactory.createStrategy(RatePlan.WEEKEND_PACKAGE, null);
        Price price = strategy.calculate(
                new Rooms().find(202).orElseThrow(),
                LocalDate.of(2027, 3, 12),
                LocalDate.of(2027, 3, 14));

        assertEquals(2, price.nights());
        assertEquals(new BigDecimal("820.00"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("820.00"), price.total());
    }

    @Test
    void longStayStandardFor7Nights() {
        RatePlanStrategy strategy = strategyFactory.createStrategy(RatePlan.LONG_STAY, null);
        Price price = strategy.calculate(
                new Rooms().find(101).orElseThrow(),
                LocalDate.of(2027, 3, 1),
                LocalDate.of(2027, 3, 8));

        assertEquals(7, price.nights());
        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void nonRefundableTakes15PercentOffRoundingHalfEven() {
        RatePlanStrategy strategy = strategyFactory.createStrategy(RatePlan.NON_REFUNDABLE, null);
        Price price = strategy.calculate(standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11));

        // 15% of 249.90 = 37.485, which rounds half-even to 37.48 (half-up would give 37.49)
        assertEquals(new BigDecimal("37.48"), price.discount());
        assertEquals(new BigDecimal("212.42"), price.total());
    }

    @Test
    void nonRefundableDiscountIsOnTheHighSeasonPrice() {
        RatePlanStrategy strategy = strategyFactory.createStrategy(RatePlan.NON_REFUNDABLE, null);
        Price price = strategy.calculate(suite, LocalDate.of(2027, 1, 5), LocalDate.of(2027, 1, 9));

        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), price.subtotal());
        assertEquals(new BigDecimal("417.53"), price.discount());
        assertEquals(new BigDecimal("2365.99"), price.total());
    }
}
