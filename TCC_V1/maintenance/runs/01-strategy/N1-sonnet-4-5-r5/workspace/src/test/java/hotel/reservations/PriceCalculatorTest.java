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
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 13), RatePlan.STANDARD, null);

        assertEquals(3, price.nights());
        assertEquals(new BigDecimal("749.70"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("749.70"), price.total());
    }

    @Test
    void highSeasonStartsOnDecember15() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD, null);

        // December 14: 249.90; December 15: 249.90 + 20% = 299.88
        assertEquals(new BigDecimal("549.78"), price.total());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 2), RatePlan.STANDARD, null);

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
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE, null);

        // 15% of 249.90 = 37.485, which rounds half-even to 37.48 (half-up would give 37.49)
        assertEquals(new BigDecimal("37.48"), price.discount());
        assertEquals(new BigDecimal("212.42"), price.total());
    }

    @Test
    void nonRefundableDiscountIsOnTheHighSeasonPrice() {
        Price price = calculator.calculate(
                suite, LocalDate.of(2027, 1, 5), LocalDate.of(2027, 1, 9), RatePlan.NON_REFUNDABLE, null);

        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), price.subtotal());
        assertEquals(new BigDecimal("417.53"), price.discount());
        assertEquals(new BigDecimal("2365.99"), price.total());
    }

    @Test
    void corporateRateTecnosulGives12PercentOff() {
        Room deluxe = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));
        Price price = calculator.calculate(
                deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "TECNOSUL");

        // 2 nights of 389.90 = 779.80
        assertEquals(new BigDecimal("779.80"), price.subtotal());
        // 12% of 779.80 = 93.576, rounds half-even to 93.58
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
    }

    @Test
    void corporateRateAgrovalGives18PercentOff() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "AGROVALE");

        // 2 nights of 249.90 = 499.80
        assertEquals(new BigDecimal("499.80"), price.subtotal());
        // 18% of 499.80 = 89.964, rounds half-even to 89.96
        assertEquals(new BigDecimal("89.96"), price.discount());
        assertEquals(new BigDecimal("409.84"), price.total());
    }

    @Test
    void corporateRateFarmavidaGives25PercentOff() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 13), RatePlan.CORPORATE, "FARMAVIDA");

        // 3 nights of 249.90 = 749.70
        assertEquals(new BigDecimal("749.70"), price.subtotal());
        // 25% of 749.70 = 187.425, rounds half-even to 187.42
        assertEquals(new BigDecimal("187.42"), price.discount());
        assertEquals(new BigDecimal("562.28"), price.total());
    }

    @Test
    void weekendPackageHasFixedPrices() {
        Room deluxe = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));
        Price standardPrice = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE, null);
        Price deluxePrice = calculator.calculate(
                deluxe, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE, null);
        Price suitePrice = calculator.calculate(
                suite, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE, null);

        assertEquals(2, standardPrice.nights());
        assertEquals(new BigDecimal("520.00"), standardPrice.total());
        assertEquals(new BigDecimal("0.00"), standardPrice.discount());

        assertEquals(new BigDecimal("820.00"), deluxePrice.total());
        assertEquals(new BigDecimal("1200.00"), suitePrice.total());
    }

    @Test
    void longStayGives10PercentOff() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), RatePlan.LONG_STAY, null);

        // 7 nights of 249.90 = 1749.30
        assertEquals(7, price.nights());
        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        // 10% of 1749.30 = 174.93
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void longStayDiscountIsOnTheHighSeasonPrice() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 17), RatePlan.LONG_STAY, null);

        // 7 nights of 249.90 + 20% = 299.88 each = 2099.16
        assertEquals(new BigDecimal("2099.16"), price.subtotal());
        // 10% of 2099.16 = 209.916, rounds half-even to 209.92
        assertEquals(new BigDecimal("209.92"), price.discount());
        assertEquals(new BigDecimal("1889.24"), price.total());
    }
}
