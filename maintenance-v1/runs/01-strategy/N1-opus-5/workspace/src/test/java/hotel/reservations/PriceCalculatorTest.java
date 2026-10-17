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
        Guest guest = new Guest("Ana Souza", "ana@example.com", "123.456.789-09");
        NewReservation request = new NewReservation(
                room.number(), guest, LocalDate.parse(checkIn), LocalDate.parse(checkOut), 1, plan, companyCode);
        return calculator.calculate(room, request);
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
    void corporateTakesTheDiscountOfTheGuestsCompany() {
        Price tecnosul = priceOf(deluxeRoom, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "TECNOSUL");

        assertEquals(new BigDecimal("779.80"), tecnosul.subtotal());
        assertEquals(new BigDecimal("93.58"), tecnosul.discount());
        assertEquals(new BigDecimal("686.22"), tecnosul.total());

        // 18% and 25% of the same two nights
        assertEquals(new BigDecimal("140.36"),
                priceOf(deluxeRoom, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "AGROVALE").discount());
        assertEquals(new BigDecimal("194.95"),
                priceOf(deluxeRoom, "2027-03-10", "2027-03-12", RatePlan.CORPORATE, "FARMAVIDA").discount());
    }

    @Test
    void theWeekendPackageCostsItsClosedPriceInAnySeason() {
        // Friday to Sunday, in low season and in high season: the same price, with no discount
        Price lowSeason = priceOf(deluxeRoom, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE);

        assertEquals(2, lowSeason.nights());
        assertEquals(new BigDecimal("820.00"), lowSeason.subtotal());
        assertEquals(new BigDecimal("0.00"), lowSeason.discount());
        assertEquals(new BigDecimal("820.00"), lowSeason.total());

        assertEquals(new BigDecimal("820.00"),
                priceOf(deluxeRoom, "2027-07-09", "2027-07-11", RatePlan.WEEKEND_PACKAGE).total());
        assertEquals(new BigDecimal("520.00"),
                priceOf(standardRoom, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE).total());
        assertEquals(new BigDecimal("1200.00"),
                priceOf(suite, "2027-03-12", "2027-03-14", RatePlan.WEEKEND_PACKAGE).total());
    }

    @Test
    void theLongStayTakes10PercentOffTheNights() {
        Price price = priceOf(standardRoom, "2027-03-01", "2027-03-08", RatePlan.LONG_STAY);

        assertEquals(7, price.nights());
        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void theLongStayDiscountIsOnTheHighSeasonPrice() {
        Price price = priceOf(standardRoom, "2027-07-01", "2027-07-08", RatePlan.LONG_STAY);

        // 7 nights of 249.90 + 20% = 299.88 each
        assertEquals(new BigDecimal("2099.16"), price.subtotal());
        assertEquals(new BigDecimal("209.92"), price.discount());
        assertEquals(new BigDecimal("1889.24"), price.total());
    }
}
