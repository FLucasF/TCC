package pousada;

import org.junit.jupiter.api.Test;
import pousada.domain.RoomType;
import pousada.domain.rateplan.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class RatePlanTest {

    @Test
    public void testStandardRatePlan() {
        RatePlan plan = new StandardRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PricingResult result = plan.calculatePricing(checkIn, checkOut, RoomType.STANDARD, null);

        assertEquals(new BigDecimal("599.76"), result.subtotal());
        assertEquals(BigDecimal.ZERO, result.discount());
        assertEquals(new BigDecimal("599.76"), result.total());
    }

    @Test
    public void testNonRefundableRatePlan() {
        RatePlan plan = new NonRefundableRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingResult result = plan.calculatePricing(checkIn, checkOut, RoomType.DELUXE, null);

        assertEquals(new BigDecimal("779.80"), result.subtotal());
        assertEquals(new BigDecimal("116.97"), result.discount());
        assertEquals(new BigDecimal("662.83"), result.total());
    }

    @Test
    public void testCorporateRatePlanTecnosul() {
        RatePlan plan = new CorporateRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingResult result = plan.calculatePricing(checkIn, checkOut, RoomType.DELUXE, "TECNOSUL");

        assertEquals(new BigDecimal("779.80"), result.subtotal());
        assertEquals(new BigDecimal("93.58"), result.discount());
        assertEquals(new BigDecimal("686.22"), result.total());
    }

    @Test
    public void testWeekendPackageDeluxe() {
        RatePlan plan = new WeekendPackageRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 14);

        PricingResult result = plan.calculatePricing(checkIn, checkOut, RoomType.DELUXE, null);

        assertEquals(new BigDecimal("820.00"), result.subtotal());
        assertEquals(BigDecimal.ZERO, result.discount());
        assertEquals(new BigDecimal("820.00"), result.total());

        BigDecimal refund = plan.calculateRefund(new BigDecimal("820.00"));
        assertEquals(new BigDecimal("410.00"), refund);
    }

    @Test
    public void testLongStayRatePlan() {
        RatePlan plan = new LongStayRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        PricingResult result = plan.calculatePricing(checkIn, checkOut, RoomType.STANDARD, null);

        assertEquals(new BigDecimal("1749.30"), result.subtotal());
        assertEquals(new BigDecimal("174.93"), result.discount());
        assertEquals(new BigDecimal("1574.37"), result.total());
    }

    @Test
    public void testWeekendPackageValidation() {
        RatePlan plan = new WeekendPackageRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        assertThrows(IllegalArgumentException.class, () -> {
            plan.validate(checkIn, checkOut, RoomType.STANDARD, null);
        });
    }

    @Test
    public void testLongStayValidation() {
        RatePlan plan = new LongStayRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        assertThrows(IllegalArgumentException.class, () -> {
            plan.validate(checkIn, checkOut, RoomType.STANDARD, null);
        });
    }

    @Test
    public void testCorporateValidation() {
        RatePlan plan = new CorporateRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        assertThrows(IllegalArgumentException.class, () -> {
            plan.validate(checkIn, checkOut, RoomType.STANDARD, null);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            plan.validate(checkIn, checkOut, RoomType.STANDARD, "INVALID");
        });
    }
}
