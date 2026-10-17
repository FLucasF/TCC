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
    private final Room suite = new Rooms().find(301).orElseThrow();

    private Price price(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan) {
        return price(room, checkIn, checkOut, plan, null);
    }

    private Price price(Room room, LocalDate checkIn, LocalDate checkOut, RatePlan plan, String companyCode) {
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        return calculator.calculate(
                room, new NewReservation(room.number(), guest, checkIn, checkOut, 1, plan, companyCode));
    }

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

    @Test
    void corporateTakesTheCompanysDiscount() {
        Room deluxe = new Rooms().find(201).orElseThrow();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        Price tecnosul = price(deluxe, checkIn, checkOut, RatePlan.CORPORATE, "TECNOSUL");
        Price agrovale = price(deluxe, checkIn, checkOut, RatePlan.CORPORATE, "AGROVALE");
        Price farmavida = price(deluxe, checkIn, checkOut, RatePlan.CORPORATE, "FARMAVIDA");

        assertEquals(new BigDecimal("779.80"), tecnosul.subtotal());
        assertEquals(new BigDecimal("93.58"), tecnosul.discount());
        assertEquals(new BigDecimal("686.22"), tecnosul.total());
        assertEquals(new BigDecimal("140.36"), agrovale.discount());
        assertEquals(new BigDecimal("194.95"), farmavida.discount());
    }

    @Test
    void theWeekendPackageHasAFixedPriceAllYearRound() {
        Room deluxe = new Rooms().find(201).orElseThrow();

        Price march = price(deluxe, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE);
        Price july = price(standardRoom, LocalDate.of(2027, 7, 9), LocalDate.of(2027, 7, 11), RatePlan.WEEKEND_PACKAGE);
        Price january = price(suite, LocalDate.of(2027, 1, 8), LocalDate.of(2027, 1, 10), RatePlan.WEEKEND_PACKAGE);

        assertEquals(2, march.nights());
        assertEquals(new BigDecimal("820.00"), march.subtotal());
        assertEquals(new BigDecimal("0.00"), march.discount());
        assertEquals(new BigDecimal("820.00"), march.total());
        assertEquals(new BigDecimal("520.00"), july.total());
        assertEquals(new BigDecimal("1200.00"), january.total());
    }

    @Test
    void longStayTakes10PercentOffTheNightsWithTheHighSeason() {
        Price march = price(standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), RatePlan.LONG_STAY);
        Price endOfJune =
                price(standardRoom, LocalDate.of(2027, 6, 28), LocalDate.of(2027, 7, 5), RatePlan.LONG_STAY);

        assertEquals(7, march.nights());
        assertEquals(new BigDecimal("1749.30"), march.subtotal());
        assertEquals(new BigDecimal("174.93"), march.discount());
        assertEquals(new BigDecimal("1574.37"), march.total());
        // June 28-30: 3 x 249.90; July 1-4: 4 x 299.88
        assertEquals(new BigDecimal("1949.22"), endOfJune.subtotal());
        assertEquals(new BigDecimal("194.92"), endOfJune.discount());
    }
}
