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
                standardRoom, stay(standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 13), RatePlan.STANDARD));

        assertEquals(3, price.nights());
        assertEquals(new BigDecimal("749.70"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("749.70"), price.total());
    }

    @Test
    void highSeasonStartsOnDecember15() {
        Price price = calculator.calculate(
                standardRoom, stay(standardRoom, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD));

        // December 14: 249.90; December 15: 249.90 + 20% = 299.88
        assertEquals(new BigDecimal("549.78"), price.total());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        Price price = calculator.calculate(
                standardRoom, stay(standardRoom, LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 2), RatePlan.STANDARD));

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
                standardRoom, stay(standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE));

        // 15% of 249.90 = 37.485, which rounds half-even to 37.48 (half-up would give 37.49)
        assertEquals(new BigDecimal("37.48"), price.discount());
        assertEquals(new BigDecimal("212.42"), price.total());
    }

    @Test
    void nonRefundableDiscountIsOnTheHighSeasonPrice() {
        Price price = calculator.calculate(
                suite, stay(suite, LocalDate.of(2027, 1, 5), LocalDate.of(2027, 1, 9), RatePlan.NON_REFUNDABLE));

        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), price.subtotal());
        assertEquals(new BigDecimal("417.53"), price.discount());
        assertEquals(new BigDecimal("2365.99"), price.total());
    }

    private final Room deluxe = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));

    private static NewReservation stay(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        return stay(room, checkIn, checkOut, plan, null);
    }

    private static NewReservation stay(
            Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan, String companyCode) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return new NewReservation(room.number(), guest, checkIn, checkOut, 2, plan, companyCode);
    }

    private static ErrorCode errorOf(Runnable action) {
        return assertThrows(HotelException.class, action::run).errorCode();
    }

    @Test
    void corporateDiscountDependsOnTheCompany() {
        Price tecnosul = calculator.calculate(deluxe, stay(
                deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "TECNOSUL"));
        Price agrovale = calculator.calculate(deluxe, stay(
                deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "AGROVALE"));
        Price farmavida = calculator.calculate(deluxe, stay(
                deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "FARMAVIDA"));

        assertEquals(new BigDecimal("779.80"), tecnosul.subtotal());
        assertEquals(new BigDecimal("93.58"), tecnosul.discount());
        assertEquals(new BigDecimal("686.22"), tecnosul.total());
        assertEquals(new BigDecimal("140.36"), agrovale.discount());
        assertEquals(new BigDecimal("194.95"), farmavida.discount());
    }

    @Test
    void corporateNeedsAKnownCompanyCode() {
        for (String code : new String[] {null, "", "tecnosul", "OUTRA"}) {
            assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> calculator.calculate(standardRoom, stay(
                    standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.CORPORATE, code))));
        }
    }

    @Test
    void weekendPackageIsAClosedPriceAllYear() {
        Price march = calculator.calculate(deluxe, stay(
                deluxe, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE));
        Price july = calculator.calculate(suite, stay(
                suite, LocalDate.of(2027, 7, 9), LocalDate.of(2027, 7, 11), RatePlan.WEEKEND_PACKAGE));
        Price january = calculator.calculate(standardRoom, stay(
                standardRoom, LocalDate.of(2027, 1, 8), LocalDate.of(2027, 1, 10), RatePlan.WEEKEND_PACKAGE));

        assertEquals(2, march.nights());
        assertEquals(new BigDecimal("820.00"), march.subtotal());
        assertEquals(new BigDecimal("0.00"), march.discount());
        assertEquals(new BigDecimal("820.00"), march.total());
        assertEquals(new BigDecimal("1200.00"), july.total());
        assertEquals(new BigDecimal("520.00"), january.total());
    }

    @Test
    void weekendPackageOnlyFromFridayToSunday() {
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> calculator.calculate(deluxe, stay(
                deluxe, LocalDate.of(2027, 3, 11), LocalDate.of(2027, 3, 13), RatePlan.WEEKEND_PACKAGE))));
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> calculator.calculate(deluxe, stay(
                deluxe, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 15), RatePlan.WEEKEND_PACKAGE))));
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> calculator.calculate(deluxe, stay(
                deluxe, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 13), RatePlan.WEEKEND_PACKAGE))));
    }

    @Test
    void longStayTakes10PercentOffFromSevenNights() {
        Price price = calculator.calculate(standardRoom, stay(
                standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), RatePlan.LONG_STAY));

        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void longStayKeepsTheHighSeasonAndRefusesShortStays() {
        Price price = calculator.calculate(standardRoom, stay(
                standardRoom, LocalDate.of(2027, 2, 25), LocalDate.of(2027, 3, 4), RatePlan.LONG_STAY));

        // February 25 to 28 are 4 high-season nights of 299.88; March 1 to 3 are 3 nights of 249.90
        assertEquals(7, price.nights());
        assertEquals(new BigDecimal("1949.22"), price.subtotal());
        assertEquals(ErrorCode.INVALID_REQUEST, errorOf(() -> calculator.calculate(standardRoom, stay(
                standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 7), RatePlan.LONG_STAY))));
    }
}
