package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private final Room deluxe = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));

    private static ErrorCode errorOf(Runnable action) {
        return assertThrows(HotelException.class, action::run).errorCode();
    }

    private Price price(Room room, String checkIn, String checkOut, RatePlan plan, String companyCode) {
        return calculator.calculate(room, LocalDate.parse(checkIn), LocalDate.parse(checkOut), plan, companyCode);
    }

    @Test
    void corporatePlanDiscountDependsOnTheCompany() {
        Price tecnosul = price(deluxe, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL");
        assertEquals(new BigDecimal("779.80"), tecnosul.subtotal());
        assertEquals(new BigDecimal("93.58"), tecnosul.discount());
        assertEquals(new BigDecimal("686.22"), tecnosul.total());
        assertEquals(new BigDecimal("639.44"),
                price(deluxe, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "AGROVALE").total());
        assertEquals(new BigDecimal("584.85"),
                price(deluxe, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "FARMAVIDA").total());
    }

    @Test
    void corporatePlanNeedsAKnownCompanyCode() {
        for (String code : new String[] {null, "ACME", "tecnosul"}) {
            assertEquals(ErrorCode.INVALID_REQUEST,
                    errorOf(() -> price(deluxe, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, code)));
        }
    }

    @Test
    void otherPlansIgnoreTheCompanyCode() {
        assertEquals(new BigDecimal("249.90"),
                price(standardRoom, "2027-03-10", "2027-03-11", RatePlan.STANDARD, "TECNOSUL").total());
    }

    @Test
    void weekendPackageIsAClosedPriceEvenInHighSeason() {
        Price deluxePrice = price(deluxe, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null);
        assertEquals(2, deluxePrice.nights());
        assertEquals(new BigDecimal("820.00"), deluxePrice.subtotal());
        assertEquals(new BigDecimal("0.00"), deluxePrice.discount());
        assertEquals(new BigDecimal("820.00"), deluxePrice.total());

        // 2027-01-08 is a Friday, in high season
        assertEquals(new BigDecimal("520.00"),
                price(standardRoom, "2027-01-08", "2027-01-10", RatePlan.WEEKEND_PACKAGE, null).total());
        assertEquals(new BigDecimal("1200.00"),
                price(suite, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE, null).total());
    }

    @Test
    void weekendPackageOnlyFromFridayToSunday() {
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> price(deluxe, "2027-03-11", "2027-03-13", RatePlan.WEEKEND_PACKAGE, null)));
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> price(deluxe, "2027-03-12", "2027-03-15", RatePlan.WEEKEND_PACKAGE, null)));
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> price(deluxe, "2027-03-12", "2027-03-13", RatePlan.WEEKEND_PACKAGE, null)));
    }

    @Test
    void longStayGivesTenPercentFromSevenNights() {
        Price price = price(standardRoom, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY, null);
        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());

        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> price(standardRoom, "2027-03-01", "2027-03-07", RatePlan.LONG_STAY, null)));
    }

    @Test
    void longStayKeepsTheHighSeasonSurcharge() {
        // 4 nights in February at 299.88 and 3 in March at 249.90
        Price price = price(standardRoom, "2027-02-25", "2027-03-04", RatePlan.LONG_STAY, null);
        assertEquals(new BigDecimal("1949.22"), price.subtotal());
    }
}
