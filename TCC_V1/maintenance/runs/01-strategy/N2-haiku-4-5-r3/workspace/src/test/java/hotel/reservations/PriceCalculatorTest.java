package hotel.reservations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hotel.rooms.Room;
import hotel.rooms.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import hotel.reservations.ReservationData;

class PriceCalculatorTest {

    private final PriceCalculator calculator = new PriceCalculator();
    private final Room standardRoom = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
    private final Room suite = new Room(301, RoomType.SUITE, 4, new BigDecimal("579.90"));

    @Test
    void lowSeasonNightsCostTheNightlyRate() {
        ReservationData data = new ReservationData(101, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 13), 2, null);
        Price price = calculator.calculate(standardRoom, data, RatePlan.STANDARD);

        assertEquals(3, price.nights());
        assertEquals(new BigDecimal("749.70"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("749.70"), price.total());
    }

    @Test
    void highSeasonStartsOnDecember15() {
        ReservationData data = new ReservationData(101, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), 2, null);
        Price price = calculator.calculate(standardRoom, data, RatePlan.STANDARD);

        // December 14: 249.90; December 15: 249.90 + 20% = 299.88
        assertEquals(new BigDecimal("549.78"), price.total());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        ReservationData data = new ReservationData(101, LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 2), 2, null);
        Price price = calculator.calculate(standardRoom, data, RatePlan.STANDARD);

        // February 29 (leap year): 299.88; March 1: 249.90
        assertEquals(new BigDecimal("549.78"), price.total());
    }


    @Test
    void nonRefundableTakes15PercentOffRoundingHalfEven() {
        ReservationData data = new ReservationData(101, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), 2, null);
        Price price = calculator.calculate(standardRoom, data, RatePlan.NON_REFUNDABLE);

        // 15% of 249.90 = 37.485, which rounds half-even to 37.48 (half-up would give 37.49)
        assertEquals(new BigDecimal("37.48"), price.discount());
        assertEquals(new BigDecimal("212.42"), price.total());
    }

    @Test
    void nonRefundableDiscountIsOnTheHighSeasonPrice() {
        ReservationData data = new ReservationData(301, LocalDate.of(2027, 1, 5), LocalDate.of(2027, 1, 9), 4, null);
        Price price = calculator.calculate(suite, data, RatePlan.NON_REFUNDABLE);

        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), price.subtotal());
        assertEquals(new BigDecimal("417.53"), price.discount());
        assertEquals(new BigDecimal("2365.99"), price.total());
    }

    @Test
    void corporateTecnosulTakes12PercentOff() {
        ReservationData data =
                new ReservationData(201, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 3, "TECNOSUL");
        Price price = calculator.calculate(
                new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")), data, RatePlan.CORPORATE);

        // 2 nights of 389.90 = 779.80, 12% discount = 93.576 ≈ 93.58
        assertEquals(new BigDecimal("779.80"), price.subtotal());
        assertEquals(new BigDecimal("93.58"), price.discount());
        assertEquals(new BigDecimal("686.22"), price.total());
    }

    @Test
    void weekendPackageFixedPriceForDeluxe() {
        ReservationData data =
                new ReservationData(201, LocalDate.of(2027, 3, 12), LocalDate.of(2027, 3, 14), 3, null);
        Price price = calculator.calculate(
                new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90")), data, RatePlan.WEEKEND_PACKAGE);

        assertEquals(2, price.nights());
        assertEquals(new BigDecimal("820.00"), price.subtotal());
        assertEquals(new BigDecimal("0.00"), price.discount());
        assertEquals(new BigDecimal("820.00"), price.total());
    }

    @Test
    void longStayWith7NightsTakes10PercentOff() {
        ReservationData data =
                new ReservationData(101, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 8), 2, null);
        Price price = calculator.calculate(standardRoom, data, RatePlan.LONG_STAY);

        // 7 nights of 249.90 = 1749.30, 10% discount = 174.93
        assertEquals(7, price.nights());
        assertEquals(new BigDecimal("1749.30"), price.subtotal());
        assertEquals(new BigDecimal("174.93"), price.discount());
        assertEquals(new BigDecimal("1574.37"), price.total());
    }
}
