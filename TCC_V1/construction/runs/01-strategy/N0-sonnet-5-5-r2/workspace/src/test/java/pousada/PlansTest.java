package pousada;

import org.junit.jupiter.api.Test;
import pousada.plan.BookingContext;
import pousada.plan.Quote;
import pousada.plan.RatePlan;
import pousada.plan.RatePlans;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PlansTest {
    private static final Room STANDARD = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
    private static final Room DELUXE = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));

    private static BookingContext ctx(Room room, String in, String out, String company) {
        return new BookingContext(room, LocalDate.parse(in), LocalDate.parse(out), 2, company);
    }

    private static Quote quote(String plan, BookingContext c) {
        RatePlan p = RatePlans.get(plan);
        p.validate(c);
        return p.quote(c);
    }

    private static BigDecimal d(String v) { return new BigDecimal(v); }

    @Test
    void highSeasonStandard() {
        Quote q = quote("STANDARD", ctx(STANDARD, "2027-07-10", "2027-07-12", null));
        assertEquals(d("599.76"), q.subtotal());
        assertEquals(d("599.76"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Season.isHigh(LocalDate.parse("2027-12-14")));
        assertTrue(Season.isHigh(LocalDate.parse("2027-12-15")));
        assertTrue(Season.isHigh(LocalDate.parse("2028-02-29")));
        assertFalse(Season.isHigh(LocalDate.parse("2028-03-01")));
        assertFalse(Season.isHigh(LocalDate.parse("2027-06-30")));
        assertTrue(Season.isHigh(LocalDate.parse("2027-07-31")));
        assertFalse(Season.isHigh(LocalDate.parse("2027-08-01")));
    }

    @Test
    void nonRefundable() {
        Quote q = quote("NON_REFUNDABLE", ctx(DELUXE, "2027-03-10", "2027-03-12", null));
        assertEquals(d("779.80"), q.subtotal());
        assertEquals(d("116.97"), q.discount());
        assertEquals(d("662.83"), q.total());
        assertEquals(d("0.00"), RatePlans.get("NON_REFUNDABLE").refund(d("662.83")));
    }

    @Test
    void corporateTecnosul() {
        Quote q = quote("CORPORATE", ctx(DELUXE, "2027-03-10", "2027-03-12", "TECNOSUL"));
        assertEquals(d("779.80"), q.subtotal());
        assertEquals(d("93.58"), q.discount());
        assertEquals(d("686.22"), q.total());
        assertEquals(d("686.22"), RatePlans.get("CORPORATE").refund(d("686.22")));
    }

    @Test
    void corporateRequiresKnownCode() {
        RatePlan p = RatePlans.get("CORPORATE");
        assertThrows(ApiException.class, () -> p.validate(ctx(DELUXE, "2027-03-10", "2027-03-12", null)));
        assertThrows(ApiException.class, () -> p.validate(ctx(DELUXE, "2027-03-10", "2027-03-12", "OUTRA")));
        assertThrows(ApiException.class, () -> p.validate(ctx(DELUXE, "2027-03-10", "2027-03-12", "tecnosul")));
    }

    @Test
    void weekendPackage() {
        Quote q = quote("WEEKEND_PACKAGE", ctx(DELUXE, "2027-03-12", "2027-03-14", null));
        assertEquals(d("820.00"), q.subtotal());
        assertEquals(d("0.00"), q.discount());
        assertEquals(d("820.00"), q.total());
        assertEquals(d("410.00"), RatePlans.get("WEEKEND_PACKAGE").refund(d("820.00")));
        // alta temporada não muda o preço fechado
        assertEquals(d("520.00"), quote("WEEKEND_PACKAGE", ctx(STANDARD, "2027-07-02", "2027-07-04", null)).total());
    }

    @Test
    void weekendPackageOnlyFridayToSunday() {
        RatePlan p = RatePlans.get("WEEKEND_PACKAGE");
        assertThrows(ApiException.class, () -> p.validate(ctx(DELUXE, "2027-03-13", "2027-03-15", null)));
        assertThrows(ApiException.class, () -> p.validate(ctx(DELUXE, "2027-03-12", "2027-03-15", null)));
        assertThrows(ApiException.class, () -> p.validate(ctx(DELUXE, "2027-03-12", "2027-03-13", null)));
    }

    @Test
    void weekendRefundRoundsHalfEven() {
        assertEquals(d("0.02"), RatePlans.get("WEEKEND_PACKAGE").refund(d("0.05")));
        assertEquals(d("0.02"), RatePlans.get("WEEKEND_PACKAGE").refund(d("0.03")));
    }

    @Test
    void longStay() {
        Quote q = quote("LONG_STAY", ctx(STANDARD, "2027-03-01", "2027-03-08", null));
        assertEquals(d("1749.30"), q.subtotal());
        assertEquals(d("174.93"), q.discount());
        assertEquals(d("1574.37"), q.total());
        assertThrows(ApiException.class,
                () -> RatePlans.get("LONG_STAY").validate(ctx(STANDARD, "2027-03-01", "2027-03-07", null)));
    }

    @Test
    void longStayKeepsHighSeasonNights() {
        // 3 noites em junho + 4 em julho
        Quote q = quote("LONG_STAY", ctx(STANDARD, "2027-06-28", "2027-07-05", null));
        assertEquals(d("1949.22"), q.subtotal()); // 3*249.90 + 4*299.88
    }

    @Test
    void receiptFormatting() {
        assertEquals("R$ 1.234,56", Money.brl(d("1234.56")));
        assertEquals("R$ 0,50", Money.brl(d("0.5")));
    }
}
