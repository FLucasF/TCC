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

    private static NewReservation stay(String checkIn, String checkOut, RatePlan plan) {
        return stay(checkIn, checkOut, plan, null);
    }

    private static NewReservation stay(String checkIn, String checkOut, RatePlan plan, String companyCode) {
        return new NewReservation(
                101, new Guest("Ana Souza", "ana@example.com", "123.456.789-09"),
                LocalDate.parse(checkIn), LocalDate.parse(checkOut), 1, plan, companyCode);
    }

    @Test
    void lowSeasonNightsCostTheNightlyRate() {
        Price price = calculator.calculate(standardRoom, stay("2027-03-10", "2027-03-13", RatePlan.STANDARD));

        assertEquals(3, price.nights());
        assertEquals(new BigDecimal("749.70"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("749.70"), price.total());
    }

    @Test
    void highSeasonStartsOnDecember15() {
        Price price = calculator.calculate(standardRoom, stay("2027-12-14", "2027-12-16", RatePlan.STANDARD));

        // December 14: 249.90; December 15: 249.90 + 20% = 299.88
        assertEquals(new BigDecimal("549.78"), price.total());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        Price price = calculator.calculate(standardRoom, stay("2028-02-29", "2028-03-02", RatePlan.STANDARD));

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
        Price price = calculator.calculate(standardRoom, stay("2027-03-10", "2027-03-11", RatePlan.NON_REFUNDABLE));

        // 15% of 249.90 = 37.485, which rounds half-even to 37.48 (half-up would give 37.49)
        assertEquals(new BigDecimal("37.48"), price.discount());
        assertEquals(new BigDecimal("212.42"), price.total());
    }

    @Test
    void nonRefundableDiscountIsOnTheHighSeasonPrice() {
        Price price = calculator.calculate(suite, stay("2027-01-05", "2027-01-09", RatePlan.NON_REFUNDABLE));

        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), price.subtotal());
        assertEquals(new BigDecimal("417.53"), price.discount());
        assertEquals(new BigDecimal("2365.99"), price.total());
    }

    @Test
    void corporateTakesTheCompanyDiscount() {
        Price price = calculator.calculate(
                deluxeRoom, stay("2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL"));

        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
    }

    @Test
    void corporateDiscountDependsOnTheCompany() {
        assertEquals(new BigDecimal("140.36"), calculator.calculate(
                deluxeRoom, stay("2027-03-10", "2027-03-12", RatePlan.CORPORATE, "AGROVALE")).discount());
        assertEquals(new BigDecimal("194.95"), calculator.calculate(
                deluxeRoom, stay("2027-03-10", "2027-03-12", RatePlan.CORPORATE, "FARMAVIDA")).discount());
    }

    @Test
    void weekendPackageIsAFixedPriceForItsRoomType() {
        Price price = calculator.calculate(
                deluxeRoom, stay("2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE));

        assertEquals(2, price.nights());
        assertEquals(new BigDecimal("820.00"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("820.00"), price.total());
    }

    @Test
    void weekendPackageIgnoresTheSeason() {
        Price price = calculator.calculate(
                suite, stay("2027-07-02", "2027-07-04", RatePlan.WEEKEND_PACKAGE));

        assertEquals(new BigDecimal("1200.00"), price.total());
    }

    @Test
    void longStayTakes10PercentOffWithHighSeasonNights() {
        Price price = calculator.calculate(
                standardRoom, stay("2027-03-01", "2027-03-08", RatePlan.LONG_STAY));

        assertEquals(7, price.nights());
        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void longStayKeepsTheHighSeasonSurcharge() {
        Price price = calculator.calculate(
                standardRoom, stay("2027-12-26", "2028-01-02", RatePlan.LONG_STAY));

        // 7 nights of 299.88 (high season) = 2099.16, 10% off = 209.92 (209.916 rounded)
        assertEquals(new BigDecimal("2099.16"), price.subtotal());
        assertEquals(new BigDecimal("209.92"), price.discount());
        assertEquals(new BigDecimal("1889.24"), price.total());
    }
}
