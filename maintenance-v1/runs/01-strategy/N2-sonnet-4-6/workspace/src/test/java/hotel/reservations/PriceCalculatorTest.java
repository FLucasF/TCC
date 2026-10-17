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

    // --- CORPORATE ---

    @Test
    void corporateTecnosulApplies12PercentDiscount() {
        // deluxe 10/03/2027–12/03/2027: 2 × 389.90 = 779.80; 12% = 93.576 → 93.58
        Price price = calculator.calculate(
                deluxeRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "TECNOSUL");

        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
    }

    @Test
    void corporateAgrovaleApplies18PercentDiscount() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "AGROVALE");

        // 2 × 249.90 = 499.80; 18% = 89.964 → 89.96
        assertEquals(new BigDecimal("89.96"), price.discount());
    }

    @Test
    void corporateFarmavideApplies25PercentDiscount() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "FARMAVIDA");

        // 2 × 249.90 = 499.80; 25% = 124.95
        assertEquals(new BigDecimal("124.95"), price.discount());
    }

    @Test
    void corporateRejectsUnknownCompanyCode() {
        HotelException ex = assertThrows(HotelException.class, () ->
                calculator.calculate(standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                        RatePlan.CORPORATE, "UNKNOWNCO"));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.errorCode());
    }

    @Test
    void corporateRejectsMissingCompanyCode() {
        HotelException ex = assertThrows(HotelException.class, () ->
                calculator.calculate(standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                        RatePlan.CORPORATE, null));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.errorCode());
    }

    // --- WEEKEND_PACKAGE ---

    @Test
    void weekendPackageUsesFixedPriceForDeluxe() {
        // 12/03/2027 is a Friday, 14/03/2027 is a Sunday
        Price price = calculator.calculate(
                deluxeRoom, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE, null);

        assertEquals(2, price.nights());
        assertEquals(new BigDecimal("820.00"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("820.00"), price.total());
    }

    @Test
    void weekendPackageUsesFixedPriceForStandard() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE, null);
        assertEquals(new BigDecimal("520.00"), price.total());
    }

    @Test
    void weekendPackageUsesFixedPriceForSuite() {
        Price price = calculator.calculate(
                suite, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE, null);
        assertEquals(new BigDecimal("1200.00"), price.total());
    }

    @Test
    void weekendPackageRejectsNonFridayCheckIn() {
        // 13/03/2027 is a Saturday
        HotelException ex = assertThrows(HotelException.class, () ->
                calculator.calculate(standardRoom, LocalDate.of(2027, 3, 13), LocalDate.of(2027, 3, 15),
                        RatePlan.WEEKEND_PACKAGE, null));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.errorCode());
    }

    @Test
    void weekendPackageRejectsNonSundayCheckOut() {
        // 12/03/2027 is Friday but checkout on Monday (15/03) instead of Sunday (14/03)
        HotelException ex = assertThrows(HotelException.class, () ->
                calculator.calculate(standardRoom, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 15),
                        RatePlan.WEEKEND_PACKAGE, null));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.errorCode());
    }

    // --- LONG_STAY ---

    @Test
    void longStayApplies10PercentDiscountForSevenNights() {
        // standard 01/03/2027–08/03/2027: 7 × 249.90 = 1749.30; 10% = 174.93
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), RatePlan.LONG_STAY, null);

        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void longStayRejectsFewerthanSevenNights() {
        HotelException ex = assertThrows(HotelException.class, () ->
                calculator.calculate(standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 7),
                        RatePlan.LONG_STAY, null));
        assertEquals(ErrorCode.INVALID_REQUEST, ex.errorCode());
    }

    @Test
    void longStayHighSeasonNightsAreCharged20PercentMore() {
        // 1 low-season night (Feb 28) + 6 high-season nights (Mar 1–6... wait that's wrong)
        // Let's use a stay that spans high season: 2027-07-01 to 2027-07-08 (7 July nights, all high)
        // 7 × (249.90 × 1.20) = 7 × 299.88 = 2099.16; 10% = 209.916 → 209.92
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 8), RatePlan.LONG_STAY, null);

        assertEquals(new BigDecimal("2099.16"), price.subtotal());
        assertEquals(new BigDecimal("209.92"), price.discount());
        assertEquals(new BigDecimal("1889.24"), price.total());
    }
}
