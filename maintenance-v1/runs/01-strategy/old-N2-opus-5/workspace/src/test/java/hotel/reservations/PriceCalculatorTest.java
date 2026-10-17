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
    private final Room deluxeRoom = new Rooms().find(201).orElseThrow();
    private final Room suite = new Rooms().find(301).orElseThrow();

    private static Stay stay(Room room, String checkIn, String checkOut) {
        return stay(room, checkIn, checkOut, null);
    }

    private static Stay stay(Room room, String checkIn, String checkOut, String companyCode) {
        return new Stay(room, LocalDate.parse(checkIn), LocalDate.parse(checkOut), companyCode);
    }

    private static ErrorCode errorOf(Runnable action) {
        return assertThrows(HotelException.class, action::run).errorCode();
    }

    @Test
    void lowSeasonNightsCostTheNightlyRate() {
        Price price = calculator.calculate(stay(standardRoom, "2027-03-10", "2027-03-13"), RatePlan.STANDARD);

        assertEquals(3, price.nights());
        assertEquals(new BigDecimal("749.70"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("749.70"), price.total());
    }

    @Test
    void highSeasonStartsOnDecember15() {
        Price price = calculator.calculate(stay(standardRoom, "2027-12-14", "2027-12-16"), RatePlan.STANDARD);

        // December 14: 249.90; December 15: 249.90 + 20% = 299.88
        assertEquals(new BigDecimal("549.78"), price.total());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        Price price = calculator.calculate(stay(standardRoom, "2028-02-29", "2028-03-02"), RatePlan.STANDARD);

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
                stay(standardRoom, "2027-03-10", "2027-03-11"), RatePlan.NON_REFUNDABLE);

        // 15% of 249.90 = 37.485, which rounds half-even to 37.48 (half-up would give 37.49)
        assertEquals(new BigDecimal("37.48"), price.discount());
        assertEquals(new BigDecimal("212.42"), price.total());
    }

    @Test
    void nonRefundableDiscountIsOnTheHighSeasonPrice() {
        Price price = calculator.calculate(stay(suite, "2027-01-05", "2027-01-09"), RatePlan.NON_REFUNDABLE);

        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), price.subtotal());
        assertEquals(new BigDecimal("417.53"), price.discount());
        assertEquals(new BigDecimal("2365.99"), price.total());
    }

    @Test
    void corporateTakesTheDiscountOfTheGuestsCompany() {
        Price tecnosul = calculator.calculate(
                stay(deluxeRoom, "2027-03-10", "2027-03-12", "TECNOSUL"), RatePlan.CORPORATE);

        assertEquals(new BigDecimal("779.80"), tecnosul.subtotal());
        assertEquals(new BigDecimal("93.58"), tecnosul.discount());
        assertEquals(new BigDecimal("686.22"), tecnosul.total());

        Stay sameStay = stay(deluxeRoom, "2027-03-10", "2027-03-12", "AGROVALE");
        assertEquals(new BigDecimal("140.36"), calculator.calculate(sameStay, RatePlan.CORPORATE).discount());
        assertEquals(
                new BigDecimal("194.95"),
                calculator.calculate(
                                stay(deluxeRoom, "2027-03-10", "2027-03-12", "FARMAVIDA"), RatePlan.CORPORATE)
                        .discount());
    }

    @Test
    void corporateNeedsTheCodeOfACompanyWithAnAgreement() {
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> RatePlan.CORPORATE.checkEligibility(
                stay(deluxeRoom, "2027-03-10", "2027-03-12"))));
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> RatePlan.CORPORATE.checkEligibility(
                stay(deluxeRoom, "2027-03-10", "2027-03-12", "tecnosul"))));
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> RatePlan.CORPORATE.checkEligibility(
                stay(deluxeRoom, "2027-03-10", "2027-03-12", "OUTRA"))));

        RatePlan.CORPORATE.checkEligibility(stay(deluxeRoom, "2027-03-10", "2027-03-12", "TECNOSUL"));
    }

    @Test
    void theWeekendPackageCostsItsClosedPrice() {
        // Friday to Sunday, in the low season and in the high season alike
        Price deluxe = calculator.calculate(stay(deluxeRoom, "2027-03-12", "2027-03-14"), RatePlan.WEEKEND_PACKAGE);

        assertEquals(2, deluxe.nights());
        assertEquals(new BigDecimal("820.00"), deluxe.subtotal());
        assertEquals(new BigDecimal("0.00"), deluxe.discount());
        assertEquals(new BigDecimal("820.00"), deluxe.total());

        assertEquals(
                new BigDecimal("520.00"),
                calculator.calculate(stay(standardRoom, "2027-07-09", "2027-07-11"), RatePlan.WEEKEND_PACKAGE)
                        .total());
        assertEquals(
                new BigDecimal("1200.00"),
                calculator.calculate(stay(suite, "2027-03-12", "2027-03-14"), RatePlan.WEEKEND_PACKAGE).total());
    }

    @Test
    void theWeekendPackageOnlyGoesFromFridayToSunday() {
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> RatePlan.WEEKEND_PACKAGE.checkEligibility(
                stay(deluxeRoom, "2027-03-13", "2027-03-15")))); // Saturday to Monday
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> RatePlan.WEEKEND_PACKAGE.checkEligibility(
                stay(deluxeRoom, "2027-03-12", "2027-03-15")))); // Friday, but three nights
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> RatePlan.WEEKEND_PACKAGE.checkEligibility(
                stay(deluxeRoom, "2027-03-12", "2027-03-13")))); // Friday, but one night

        RatePlan.WEEKEND_PACKAGE.checkEligibility(stay(deluxeRoom, "2027-03-12", "2027-03-14"));
    }

    @Test
    void theLongStayTakes10PercentOffSevenNightsOrMore() {
        Price price = calculator.calculate(stay(standardRoom, "2027-03-01", "2027-03-08"), RatePlan.LONG_STAY);

        assertEquals(7, price.nights());
        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void theLongStayDiscountIsOnTheHighSeasonPrice() {
        Price price = calculator.calculate(stay(standardRoom, "2027-06-28", "2027-07-05"), RatePlan.LONG_STAY);

        // 3 nights of June at 249.90 and 4 nights of July at 299.88
        assertEquals(new BigDecimal("1949.22"), price.subtotal());
        assertEquals(new BigDecimal("194.92"), price.discount());
        assertEquals(new BigDecimal("1754.30"), price.total());
    }

    @Test
    void theLongStayNeedsSevenNights() {
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> RatePlan.LONG_STAY.checkEligibility(
                stay(standardRoom, "2027-03-01", "2027-03-07"))));

        RatePlan.LONG_STAY.checkEligibility(stay(standardRoom, "2027-03-01", "2027-03-08"));
    }
}
