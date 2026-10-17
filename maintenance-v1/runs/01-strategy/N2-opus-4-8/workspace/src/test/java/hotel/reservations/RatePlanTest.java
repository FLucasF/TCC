package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hotel.rooms.Room;
import hotel.rooms.Rooms;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RatePlanTest {

    private final PriceCalculator calculator = new PriceCalculator();
    private final Rooms rooms = new Rooms();
    private final Room standardRoom = rooms.find(101).orElseThrow();
    private final Room deluxe = rooms.find(201).orElseThrow();

    private RateRequest stay(Room room, String checkIn, String checkOut, String companyCode) {
        return new RateRequest(room, LocalDate.parse(checkIn), LocalDate.parse(checkOut), companyCode);
    }

    private Price price(RatePlan plan, RateRequest request) {
        plan.checkEligible(request);
        return plan.price(calculator, request);
    }

    private static ErrorCode errorOf(Runnable action) {
        return assertThrows(HotelException.class, action::run).errorCode();
    }

    @Test
    void corporateDiscountsByTheCompanyCode() {
        // Tecnosul (12%), deluxe, 10/03/2027 to 12/03/2027
        Price price = price(RatePlan.CORPORATE, stay(deluxe, "2027-03-10", "2027-03-12", "TECNOSUL"));

        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
    }

    @Test
    void corporateHonoursEachCompanysOwnRate() {
        RateRequest agrovale = stay(deluxe, "2027-03-10", "2027-03-12", "AGROVALE");
        RateRequest farmavida = stay(deluxe, "2027-03-10", "2027-03-12", "FARMAVIDA");

        assertEquals(new BigDecimal("140.36"), price(RatePlan.CORPORATE, agrovale).discount()); // 18%
        assertEquals(new BigDecimal("194.95"), price(RatePlan.CORPORATE, farmavida).discount()); // 25%
    }

    @Test
    void corporateNeedsAKnownCompanyCode() {
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> RatePlan.CORPORATE.checkEligible(stay(deluxe, "2027-03-10", "2027-03-12", null))));
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> RatePlan.CORPORATE.checkEligible(stay(deluxe, "2027-03-10", "2027-03-12", "OUTRA"))));
    }

    @Test
    void corporateGivesEverythingBackOnCancellation() {
        assertEquals(new BigDecimal("686.22"), RatePlan.CORPORATE.refund(new BigDecimal("686.22")));
    }

    @Test
    void weekendPackageIsAFlatPriceAndHalfComesBack() {
        // Friday 12/03/2027 to Sunday 14/03/2027, deluxe
        Price price = price(RatePlan.WEEKEND_PACKAGE, stay(deluxe, "2027-03-12", "2027-03-14", null));

        assertEquals(2, price.nights());
        assertEquals(new BigDecimal("820.00"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("820.00"), price.total());
        assertEquals(new BigDecimal("410.00"), RatePlan.WEEKEND_PACKAGE.refund(new BigDecimal("820.00")));
    }

    @Test
    void weekendPackageRunsOnlyFridayToSunday() {
        assertEquals(ErrorCode.INVALID_REQUEST, // starts on a Thursday
                errorOf(() -> RatePlan.WEEKEND_PACKAGE.checkEligible(stay(deluxe, "2027-03-11", "2027-03-13", null))));
        assertEquals(ErrorCode.INVALID_REQUEST, // a Friday, but three nights
                errorOf(() -> RatePlan.WEEKEND_PACKAGE.checkEligible(stay(deluxe, "2027-03-12", "2027-03-15", null))));
    }

    @Test
    void longStayTakes10PercentFromSevenNightsOn() {
        // Standard, 01/03/2027 to 08/03/2027, seven nights
        Price price = price(RatePlan.LONG_STAY, stay(standardRoom, "2027-03-01", "2027-03-08", null));

        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void longStayNeedsAtLeastSevenNights() {
        assertEquals(ErrorCode.INVALID_REQUEST,
                errorOf(() -> RatePlan.LONG_STAY.checkEligible(stay(standardRoom, "2027-03-01", "2027-03-07", null))));
    }
}
