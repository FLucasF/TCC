package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 13), RatePlan.STANDARD);

        assertEquals(3, price.nights());
        assertEquals(new BigDecimal("749.70"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("749.70"), price.total());
    }

    @Test
    void highSeasonStartsOnDecember15() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD);

        // December 14: 249.90; December 15: 249.90 + 20% = 299.88
        assertEquals(new BigDecimal("549.78"), price.total());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        Price price = calculator.calculate(
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
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE);

        // 15% of 249.90 = 37.485, which rounds half-even to 37.48 (half-up would give 37.49)
        assertEquals(new BigDecimal("37.48"), price.discount());
        assertEquals(new BigDecimal("212.42"), price.total());
    }

    @Test
    void nonRefundableDiscountIsOnTheHighSeasonPrice() {
        Price price = calculator.calculate(
                suite, LocalDate.of(2027, 1, 5), LocalDate.of(2027, 1, 9), RatePlan.NON_REFUNDABLE);

        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), price.subtotal());
        assertEquals(new BigDecimal("417.53"), price.discount());
        assertEquals(new BigDecimal("2365.99"), price.total());
    }

    @Test
    void corporateDiscountByCompany() {
        Room deluxe = new Room(201, RoomType.DELUXE, 2, new BigDecimal("389.90"));
        Price tecnosul = calculator.calculate(
                deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "TECNOSUL");
        // 2 nights of 389.90 = 779.80, 12% discount = 93.58
        assertEquals(new BigDecimal("779.80"), tecnosul.subtotal());
        assertEquals(new BigDecimal("93.58"), tecnosul.discount());
        assertEquals(new BigDecimal("686.22"), tecnosul.total());

        Price agrovale = calculator.calculate(
                deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "AGROVALE");
        // 18% discount = 140.36
        assertEquals(new BigDecimal("639.44"), agrovale.total());

        Price farmavida = calculator.calculate(
                deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "FARMAVIDA");
        // 25% discount = 194.95
        assertEquals(new BigDecimal("584.85"), farmavida.total());
    }

    @Test
    void weekendPackageFixedPrice() {
        Room standard = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
        Room deluxe = new Room(201, RoomType.DELUXE, 2, new BigDecimal("409.90"));
        Room suite = new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90"));

        Price standardPrice = calculator.calculate(
                standard, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE);
        assertEquals(2, standardPrice.nights());
        assertEquals(new BigDecimal("520.00"), standardPrice.subtotal());
        assertEquals(new BigDecimal("520.00"), standardPrice.total());

        Price deluxePrice = calculator.calculate(
                deluxe, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE);
        assertEquals(2, deluxePrice.nights());
        assertEquals(new BigDecimal("820.00"), deluxePrice.total());

        Price suitePrice = calculator.calculate(
                suite, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE);
        assertEquals(2, suitePrice.nights());
        assertEquals(new BigDecimal("1200.00"), suitePrice.total());
    }

    @Test
    void longStayDiscount() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), RatePlan.LONG_STAY);

        // 7 nights of 249.90 = 1749.30, 10% discount = 174.93
        assertEquals(7, price.nights());
        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }

    @Test
    void longStayWithHighSeason() {
        Price price = calculator.calculate(
                standardRoom, LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 8), RatePlan.LONG_STAY);

        // 7 nights of 249.90 + 20% = 299.88 each = 2099.16, 10% discount = 209.92
        assertEquals(7, price.nights());
        assertEquals(new BigDecimal("2099.16"), price.subtotal());
        assertEquals(new BigDecimal("209.92"), price.discount());
        assertEquals(new BigDecimal("1889.24"), price.total());
    }

    @Test
    void weekendPackageIsValidOnlyFridayToSunday() {
        assertTrue(PriceCalculator.isWeekendPackageValid(
                LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14)));
        assertFalse(PriceCalculator.isWeekendPackageValid(
                LocalDate.of(2027, 3, 11), LocalDate.of(2027, 3, 13)));
        assertFalse(PriceCalculator.isWeekendPackageValid(
                LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 15)));
    }

    @Test
    void validatesUserProvidedNumbers() {
        // Corporate Tecnosul: deluxe from 10/03 to 12/03 = R$ 779,80 (subtotal), R$ 93,58 (discount), R$ 686,22 (total)
        Room deluxe = new Room(201, RoomType.DELUXE, 2, new BigDecimal("389.90"));
        Price corporate = calculator.calculate(
                deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.CORPORATE, "TECNOSUL");
        assertEquals(new BigDecimal("779.80"), corporate.subtotal());
        assertEquals(new BigDecimal("93.58"), corporate.discount());
        assertEquals(new BigDecimal("686.22"), corporate.total());

        // Weekend package deluxe: Friday 12/03 to Sunday 14/03 = R$ 820,00
        Price weekend = calculator.calculate(
                deluxe, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), RatePlan.WEEKEND_PACKAGE);
        assertEquals(new BigDecimal("820.00"), weekend.total());

        // Long stay standard: 01/03 to 08/03 (7 nights) = R$ 1.749,30 (subtotal), R$ 174,93 (discount), R$ 1.574,37 (total)
        Price longStay = calculator.calculate(
                standardRoom, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), RatePlan.LONG_STAY);
        assertEquals(new BigDecimal("1749.30"), longStay.subtotal());
        assertEquals(new BigDecimal("174.93"), longStay.discount());
        assertEquals(new BigDecimal("1574.37"), longStay.total());
    }
}
