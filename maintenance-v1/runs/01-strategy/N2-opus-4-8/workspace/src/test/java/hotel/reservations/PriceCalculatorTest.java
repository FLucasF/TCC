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

    @Test
    void lowSeasonNightsCostTheNightlyRate() {
        Price base = calculator.base(standardRoom, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 13));

        assertEquals(3, base.nights());
        assertEquals(new BigDecimal("749.70"), base.subtotal());
        assertEquals(new BigDecimal("0.00"), base.discount());
        assertEquals(new BigDecimal("749.70"), base.total());
    }

    @Test
    void highSeasonStartsOnDecember15() {
        Price base = calculator.base(standardRoom, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16));

        // December 14: 249.90; December 15: 249.90 + 20% = 299.88
        assertEquals(new BigDecimal("549.78"), base.subtotal());
    }

    @Test
    void highSeasonEndsWithFebruary() {
        Price base = calculator.base(standardRoom, LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 2));

        // February 29 (leap year): 299.88; March 1: 249.90
        assertEquals(new BigDecimal("549.78"), base.subtotal());
    }

    @Test
    void julyIsHighSeason() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 6, 30)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void highSeasonSurchargeFeedsThePlanDiscount() {
        // The base carries the high-season surcharge, so the plan's discount is on the high-season price.
        Price base = calculator.base(suite, LocalDate.of(2027, 1, 5), LocalDate.of(2027, 1, 9));

        // 4 nights of 579.90 + 20% = 695.88 each
        assertEquals(new BigDecimal("2783.52"), base.subtotal());
    }
}
