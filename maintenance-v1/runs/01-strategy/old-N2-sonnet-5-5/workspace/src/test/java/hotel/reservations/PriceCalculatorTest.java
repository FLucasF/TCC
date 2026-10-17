package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hotel.rooms.Room;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PriceCalculatorTest {

    private final PriceCalculator calculator = new PriceCalculator();
    private final Room standardRoom = new Rooms().find(101).orElseThrow();
    private final Room suite = new Rooms().find(301).orElseThrow();

    private Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        return calculate(room, checkIn, checkOut, plan, null);
    }

    private Price calculate(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan, String companyCode) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return calculator.calculate(room, new NewReservation(room.number(), guest, checkIn, checkOut, 1, plan, companyCode));
    }

    @Test
    void lowSeasonNightsCostTheNightlyRate() {
        Price price = calculate(
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 13), RatePlan.STANDARD);

        assertEquals(3, price.nights());
        assertEquals(new BigDecimal("749.70"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("749.70"), price.total());
    }

    @Test
    void highSeasonStartsOnDecember15() {
        Price price = calculate(
                standardRoom, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD);

        // December 14: 249.90; December 15: 249.90 + 20% = 299.88
        assertEquals(new BigDecimal("549.78"), price.total());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        Price price = calculate(
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
        Price price = calculate(
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE);

        // 15% of 249.90 = 37.485, which rounds half-even to 37.48 (half-up would give 37.49)
        assertEquals(new BigDecimal("37.48"), price.discount());
        assertEquals(new BigDecimal("212.42"), price.total());
    }

    @Test
    void nonRefundableDiscountIsOnTheHighSeasonPrice() {
        Price price = calculate(
                suite, LocalDate.of(2027, 1, 5), LocalDate.of(2027, 1, 9), RatePlan.NON_REFUNDABLE);

        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), price.subtotal());
        assertEquals(new BigDecimal("417.53"), price.discount());
        assertEquals(new BigDecimal("2365.99"), price.total());
    }

    private static ErrorCode errorOf(Runnable action) {
        return assertThrows(HotelException.class, action::run).errorCode();
    }

    @Test
    void corporateRateGivesTheCompanyDiscount() {
        Room deluxe = new Rooms().find(201).orElseThrow();

        Price price = calculate(deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "TECNOSUL");

        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
    }

    @Test
    void corporateRateNeedsAKnownCompanyCode() {
        LocalDate in = LocalDate.of(2027, 3, 10);
        LocalDate out = LocalDate.of(2027, 3, 12);

        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> calculate(standardRoom, in, out, RatePlan.CORPORATE, null)));
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> calculate(standardRoom, in, out, RatePlan.CORPORATE, "")));
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> calculate(standardRoom, in, out, RatePlan.CORPORATE, "tecnosul")));
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> calculate(standardRoom, in, out, RatePlan.CORPORATE, "OUTRA")));
        assertEquals(new BigDecimal("409.84"),
                calculate(standardRoom, in, out, RatePlan.CORPORATE, "AGROVALE").total());
        assertEquals(new BigDecimal("374.85"),
                calculate(standardRoom, in, out, RatePlan.CORPORATE, "FARMAVIDA").total());
    }

    @Test
    void otherPlansIgnoreTheCompanyCode() {
        Price price = calculate(
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.STANDARD, "TECNOSUL");

        assertEquals(new BigDecimal("249.90"), price.total());
    }

    @Test
    void weekendPackageHasAClosedPriceAllYear() {
        Room deluxe = new Rooms().find(201).orElseThrow();

        Price march = calculate(deluxe, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE);
        Price july = calculate(standardRoom, LocalDate.of(2027, 7, 9), LocalDate.of(2027, 7, 11), RatePlan.WEEKEND_PACKAGE);
        Price suitePrice = calculate(suite, LocalDate.of(2027, 1, 8), LocalDate.of(2027, 1, 10), RatePlan.WEEKEND_PACKAGE);

        assertEquals(new BigDecimal("820.00"), march.subtotal());
        assertEquals(new BigDecimal("0.00"), march.discount());
        assertEquals(new BigDecimal("820.00"), march.total());
        assertEquals(2, march.nights());
        assertEquals(new BigDecimal("520.00"), july.total());
        assertEquals(new BigDecimal("1200.00"), suitePrice.total());
    }

    @Test
    void weekendPackageOnlyFromFridayToSunday() {
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> calculate(
                standardRoom, LocalDate.of(2027, 3, 11), LocalDate.of(2027, 3, 13), RatePlan.WEEKEND_PACKAGE)));
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> calculate(
                standardRoom, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 15), RatePlan.WEEKEND_PACKAGE)));
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> calculate(
                standardRoom, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 13), RatePlan.WEEKEND_PACKAGE)));
    }

    @Test
    void longStayGivesTenPercentFromSevenNights() {
        Price price = calculate(standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), RatePlan.LONG_STAY);

        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> calculate(
                standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 7), RatePlan.LONG_STAY)));
    }

    @Test
    void longStayKeepsTheHighSeasonSurcharge() {
        Price price = calculate(standardRoom, LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 8), RatePlan.LONG_STAY);

        assertEquals(new BigDecimal("2099.16"), price.subtotal());
        assertEquals(new BigDecimal("209.92"), price.discount());
    }

    @Test
    void refundsFollowThePlan() {
        BigDecimal paid = new BigDecimal("820.00");

        assertEquals(paid, RatePlan.STANDARD.refund(paid));
        assertEquals(paid, RatePlan.CORPORATE.refund(paid));
        assertEquals(paid, RatePlan.LONG_STAY.refund(paid));
        assertEquals(new BigDecimal("410.00"), RatePlan.WEEKEND_PACKAGE.refund(paid));
        assertEquals(new BigDecimal("0.00"), RatePlan.WEEKEND_PACKAGE.refund(new BigDecimal("0.00")));
        assertEquals(new BigDecimal("0.00"), RatePlan.NON_REFUNDABLE.refund(paid));
    }
}
