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
    private final Room deluxeRoom = new Rooms().find(201).orElseThrow();
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
    void corporateTakesTheCompanyDiscountOffTheNights() {
        Price price = calculator.calculate(
                new Stay(deluxeRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), "TECNOSUL"),
                RatePlan.CORPORATE);

        // 2 nights of 389.90 = 779.80, 12% of which is 93.576, rounding half-even to 93.58
        assertEquals(2, price.nights());
        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
    }

    @Test
    void everyCompanyHasItsOwnDiscount() {
        assertEquals(new BigDecimal("140.36"), corporateDiscount("AGROVALE"));
        assertEquals(new BigDecimal("194.95"), corporateDiscount("FARMAVIDA"));
    }

    @Test
    void theWeekendPackageCostsItsClosedPriceAllYear() {
        Price lowSeason = calculator.calculate(
                new Stay(deluxeRoom, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), null),
                RatePlan.WEEKEND_PACKAGE);

        assertEquals(2, lowSeason.nights());
        assertEquals(new BigDecimal("820.00"), lowSeason.subtotal());
        assertEquals(new BigDecimal("0.00"), lowSeason.discount());
        assertEquals(new BigDecimal("820.00"), lowSeason.total());

        // July is high season, and the package price does not move
        Price highSeason = calculator.calculate(
                new Stay(deluxeRoom, LocalDate.of(2027, 7, 9), LocalDate.of(2027, 7, 11), null),
                RatePlan.WEEKEND_PACKAGE);
        assertEquals(new BigDecimal("820.00"), highSeason.total());
    }

    @Test
    void theWeekendPackageHasAPriceForEveryRoomType() {
        assertEquals(new BigDecimal("520.00"), weekendPackageTotal(standardRoom));
        assertEquals(new BigDecimal("1200.00"), weekendPackageTotal(suite));
    }

    @Test
    void theLongStayTakes10PercentOffTheNights() {
        Price price = calculator.calculate(
                new Stay(standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), null),
                RatePlan.LONG_STAY);

        // 7 nights of 249.90 = 1749.30, 10% of which is 174.93
        assertEquals(7, price.nights());
        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void theLongStayDiscountIsOnTheHighSeasonPrice() {
        Price price = calculator.calculate(
                new Stay(standardRoom, LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 8), null),
                RatePlan.LONG_STAY);

        // 7 nights of 249.90 + 20% = 299.88 each
        assertEquals(new BigDecimal("2099.16"), price.subtotal());
        assertEquals(new BigDecimal("209.92"), price.discount());
        assertEquals(new BigDecimal("1889.24"), price.total());
    }

    private BigDecimal corporateDiscount(String companyCode) {
        return calculator.calculate(
                        new Stay(deluxeRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), companyCode),
                        RatePlan.CORPORATE)
                .discount();
    }

    private BigDecimal weekendPackageTotal(Room room) {
        return calculator.calculate(
                        new Stay(room, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), null),
                        RatePlan.WEEKEND_PACKAGE)
                .total();
    }
}
