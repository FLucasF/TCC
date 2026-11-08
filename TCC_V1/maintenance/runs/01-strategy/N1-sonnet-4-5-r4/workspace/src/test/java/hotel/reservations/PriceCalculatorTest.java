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
    private final Room deluxeRoom = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));
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
    void corporateTecnosulGives12PercentOff() {
        Price price = calculator.calculate(
                deluxeRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "TECNOSUL");

        // 2 nights of 389.90 each = 779.80
        assertEquals(new BigDecimal("779.80"), price.subtotal());
        // 12% of 779.80 = 93.576, rounds to 93.58
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
    }

    @Test
    void corporateAgrovaleGives18PercentOff() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.CORPORATE, "AGROVALE");

        // 18% of 249.90 = 44.982, rounds to 44.98
        assertEquals(new BigDecimal("44.98"), price.discount());
        assertEquals(new BigDecimal("204.92"), price.total());
    }

    @Test
    void corporateFarmavidaGives25PercentOff() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.CORPORATE, "FARMAVIDA");

        // 25% of 249.90 = 62.475, rounds to 62.48
        assertEquals(new BigDecimal("62.48"), price.discount());
        assertEquals(new BigDecimal("187.42"), price.total());
    }

    @Test
    void corporateRequiresAValidCompanyCode() {
        HotelException missing = assertThrows(HotelException.class, () ->
                calculator.calculate(standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11),
                        RatePlan.CORPORATE, null));
        assertEquals(ErrorCode.INVALID_REQUEST, missing.errorCode());

        HotelException unknown = assertThrows(HotelException.class, () ->
                calculator.calculate(standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11),
                        RatePlan.CORPORATE, "UNKNOWN"));
        assertEquals(ErrorCode.INVALID_REQUEST, unknown.errorCode());
    }

    @Test
    void weekendPackageHasFixedPriceByRoomType() {
        // Friday March 12 to Sunday March 14, 2027
        Price standard = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE, null);
        assertEquals(new BigDecimal("520.00"), standard.total());

        Price deluxe = calculator.calculate(
                deluxeRoom, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE, null);
        assertEquals(new BigDecimal("820.00"), deluxe.total());

        Price suitePrice = calculator.calculate(
                suite, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE, null);
        assertEquals(new BigDecimal("1200.00"), suitePrice.total());
    }

    @Test
    void weekendPackageRequiresFridayToSunday() {
        // Not Friday to Sunday
        HotelException notFriday = assertThrows(HotelException.class, () ->
                calculator.calculate(standardRoom, LocalDate.of(2027, 3, 13), LocalDate.of(2027, 3, 15),
                        RatePlan.WEEKEND_PACKAGE, null));
        assertEquals(ErrorCode.INVALID_REQUEST, notFriday.errorCode());
    }

    @Test
    void weekendPackageRequiresExactlyTwoNights() {
        // Friday to Monday (3 nights)
        HotelException tooLong = assertThrows(HotelException.class, () ->
                calculator.calculate(standardRoom, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 15),
                        RatePlan.WEEKEND_PACKAGE, null));
        assertEquals(ErrorCode.INVALID_REQUEST, tooLong.errorCode());
    }

    @Test
    void longStayGives10PercentOffFor7OrMoreNights() {
        // 7 nights: March 1 to March 8, 2027 (7 nights at 249.90 = 1749.30)
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), RatePlan.LONG_STAY, null);

        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        // 10% of 1749.30 = 174.93
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void longStayRequiresAtLeast7Nights() {
        HotelException tooShort = assertThrows(HotelException.class, () ->
                calculator.calculate(standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 16),
                        RatePlan.LONG_STAY, null));
        assertEquals(ErrorCode.INVALID_REQUEST, tooShort.errorCode());
    }

    @Test
    void longStayAppliesHighSeasonSurcharge() {
        // 7 nights in July (high season): each night is 249.90 + 20% = 299.88
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 8), RatePlan.LONG_STAY, null);

        // 7 * 299.88 = 2099.16
        assertEquals(new BigDecimal("2099.16"), price.subtotal());
        // 10% of 2099.16 = 209.916, rounds to 209.92
        assertEquals(new BigDecimal("209.92"), price.discount());
        assertEquals(new BigDecimal("1889.24"), price.total());
    }
}
