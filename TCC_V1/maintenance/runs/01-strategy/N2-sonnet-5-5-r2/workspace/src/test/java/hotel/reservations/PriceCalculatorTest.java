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
        Price price = price(
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 13), RatePlan.STANDARD);

        assertEquals(3, price.nights());
        assertEquals(new BigDecimal("749.70"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("749.70"), price.total());
    }

    @Test
    void highSeasonStartsOnDecember15() {
        Price price = price(
                standardRoom, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD);

        // December 14: 249.90; December 15: 249.90 + 20% = 299.88
        assertEquals(new BigDecimal("549.78"), price.total());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        Price price = price(
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
        Price price = price(
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE);

        // 15% of 249.90 = 37.485, which rounds half-even to 37.48 (half-up would give 37.49)
        assertEquals(new BigDecimal("37.48"), price.discount());
        assertEquals(new BigDecimal("212.42"), price.total());
    }

    @Test
    void nonRefundableDiscountIsOnTheHighSeasonPrice() {
        Price price = price(
                suite, LocalDate.of(2027, 1, 5), LocalDate.of(2027, 1, 9), RatePlan.NON_REFUNDABLE);

        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), price.subtotal());
        assertEquals(new BigDecimal("417.53"), price.discount());
        assertEquals(new BigDecimal("2365.99"), price.total());
    }

    private final Room deluxe = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));

    private Price price(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        return priceWithCompany(room, checkIn, checkOut, plan, null);
    }

    private Price priceWithCompany(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan, String company) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return calculator.calculate(room, new NewReservation(room.number(), guest, checkIn, checkOut, 1, plan, company));
    }

    private static ErrorCode rejection(Runnable pricing) {
        return assertThrows(HotelException.class, pricing::run).errorCode();
    }

    @Test
    void corporateGivesTheDiscountOfTheCompany() {
        Price price = priceWithCompany(
                deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "TECNOSUL");

        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
        assertEquals(new BigDecimal("140.36"), priceWithCompany(
                deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "AGROVALE")
                .discount());
        assertEquals(new BigDecimal("194.95"), priceWithCompany(
                deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "FARMAVIDA")
                .discount());
    }

    @Test
    void corporateNeedsAKnownCompanyCode() {
        for (String code : new String[] {null, "", "tecnosul", "OUTRA"}) {
            assertEquals(ErrorCode.INVALID_REQUEST, rejection(() -> priceWithCompany(
                    deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, code)));
        }
    }

    @Test
    void otherPlansIgnoreTheCompanyCode() {
        Price price = priceWithCompany(
                deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.STANDARD, "NADA");

        assertEquals(new BigDecimal("779.80"), price.total());
    }

    @Test
    void weekendPackageIsAClosedPriceInAnySeason() {
        Price march = price(deluxe, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE);
        Price january = price(suite, LocalDate.of(2027, 1, 8), LocalDate.of(2027, 1, 10), RatePlan.WEEKEND_PACKAGE);
        Price standard = price(
                standardRoom, LocalDate.of(2027, 7, 9), LocalDate.of(2027, 7, 11), RatePlan.WEEKEND_PACKAGE);

        assertEquals(2, march.nights());
        assertEquals(new BigDecimal("820.00"), march.subtotal());
        assertEquals(new BigDecimal("0.00"), march.discount());
        assertEquals(new BigDecimal("820.00"), march.total());
        assertEquals(new BigDecimal("1200.00"), january.total());
        assertEquals(new BigDecimal("520.00"), standard.total());
    }

    @Test
    void weekendPackageOnlyFromFridayToTheNextSunday() {
        // Thursday to Saturday, Friday to Monday, Friday to Saturday
        assertEquals(ErrorCode.INVALID_REQUEST, rejection(() -> price(
                deluxe, LocalDate.of(2027, 3, 11), LocalDate.of(2027, 3, 13), RatePlan.WEEKEND_PACKAGE)));
        assertEquals(ErrorCode.INVALID_REQUEST, rejection(() -> price(
                deluxe, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 15), RatePlan.WEEKEND_PACKAGE)));
        assertEquals(ErrorCode.INVALID_REQUEST, rejection(() -> price(
                deluxe, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 13), RatePlan.WEEKEND_PACKAGE)));
        // Friday to the Sunday of the next week
        assertEquals(ErrorCode.INVALID_REQUEST, rejection(() -> price(
                deluxe, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 21), RatePlan.WEEKEND_PACKAGE)));
    }

    @Test
    void longStayTakes10PercentOff() {
        Price price = price(standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), RatePlan.LONG_STAY);

        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void longStayKeepsHighSeasonOnItsNights() {
        // Feb 27, 28 at 299.88 and Mar 1..5 at 249.90: 599.76 + 1249.50 = 1849.26
        Price price = price(standardRoom, LocalDate.of(2027, 2, 27), LocalDate.of(2027, 3, 6), RatePlan.LONG_STAY);

        assertEquals(new BigDecimal("1849.26"), price.subtotal());
    }

    @Test
    void longStayNeedsSevenNights() {
        assertEquals(ErrorCode.INVALID_REQUEST, rejection(() -> price(
                standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 7), RatePlan.LONG_STAY)));
    }
}
