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

    private Price priceOf(Room room, String checkIn, String checkOut, RatePlan plan) {
        return priceOf(room, checkIn, checkOut, plan, null);
    }

    private Price priceOf(Room room, String checkIn, String checkOut, RatePlan plan, String companyCode) {
        return calculator.calculate(
                new Stay(room, LocalDate.parse(checkIn), LocalDate.parse(checkOut), plan, companyCode));
    }

    @Test
    void lowSeasonNightsCostTheNightlyRate() {
        Price price = priceOf(standardRoom, "2027-03-10", "2027-03-13", RatePlan.STANDARD);

        assertEquals(3, price.nights());
        assertEquals(new BigDecimal("749.70"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("749.70"), price.total());
    }

    @Test
    void highSeasonStartsOnDecember15() {
        Price price = priceOf(standardRoom, "2027-12-14", "2027-12-16", RatePlan.STANDARD);

        // December 14: 249.90; December 15: 249.90 + 20% = 299.88
        assertEquals(new BigDecimal("549.78"), price.total());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        Price price = priceOf(standardRoom, "2028-02-29", "2028-03-02", RatePlan.STANDARD);

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
        Price price = priceOf(standardRoom, "2027-03-10", "2027-03-11", RatePlan.NON_REFUNDABLE);

        // 15% of 249.90 = 37.485, which rounds half-even to 37.48 (half-up would give 37.49)
        assertEquals(new BigDecimal("37.48"), price.discount());
        assertEquals(new BigDecimal("212.42"), price.total());
    }

    @Test
    void nonRefundableDiscountIsOnTheHighSeasonPrice() {
        Price price = priceOf(suite, "2027-01-05", "2027-01-09", RatePlan.NON_REFUNDABLE);

        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), price.subtotal());
        assertEquals(new BigDecimal("417.53"), price.discount());
        assertEquals(new BigDecimal("2365.99"), price.total());
    }

    @Test
    void corporateTakesTheDiscountAgreedWithTheCompany() {
        Price tecnosul = priceOf(deluxeRoom, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL");

        // 2 nights of 389.90 = 779.80; 12% of it = 93.576, half-even 93.58
        assertEquals(new BigDecimal("779.80"), tecnosul.subtotal());
        assertEquals(new BigDecimal("93.58"), tecnosul.discount());
        assertEquals(new BigDecimal("686.22"), tecnosul.total());

        Price agrovale = priceOf(deluxeRoom, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "AGROVALE");
        assertEquals(new BigDecimal("140.36"), agrovale.discount());

        Price farmavida = priceOf(deluxeRoom, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "FARMAVIDA");
        assertEquals(new BigDecimal("194.95"), farmavida.discount());
    }

    @Test
    void theWeekendPackageCostsItsClosedPrice() {
        // Friday March 12, 2027 to Sunday March 14
        Price standard = priceOf(standardRoom, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE);

        assertEquals(2, standard.nights());
        assertEquals(new BigDecimal("520.00"), standard.subtotal());
        assertEquals(new BigDecimal("0.00"), standard.discount());
        assertEquals(new BigDecimal("520.00"), standard.total());

        assertEquals(
                new BigDecimal("820.00"),
                priceOf(deluxeRoom, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE).total());
        assertEquals(
                new BigDecimal("1200.00"),
                priceOf(suite, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE).total());
    }

    @Test
    void theWeekendPackagePriceIsTheSameInHighSeason() {
        // Friday July 9, 2027 to Sunday July 11, high season
        Price highSeason = priceOf(deluxeRoom, "2027-07-09", "2027-07-11", RatePlan.WEEKEND_PACKAGE);

        assertEquals(new BigDecimal("820.00"), highSeason.total());
    }

    @Test
    void longStayTakes10PercentOffTheNights() {
        Price price = priceOf(standardRoom, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY);

        // 7 nights of 249.90 = 1749.30; 10% of it = 174.93
        assertEquals(7, price.nights());
        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void longStayDiscountIsOnTheHighSeasonPrice() {
        Price price = priceOf(standardRoom, "2027-06-28", "2027-07-05", RatePlan.LONG_STAY);

        // 3 nights of 249.90 (June) + 4 nights of 299.88 (July) = 1949.22
        assertEquals(new BigDecimal("1949.22"), price.subtotal());
        assertEquals(new BigDecimal("194.92"), price.discount());
        assertEquals(new BigDecimal("1754.30"), price.total());
    }
}
