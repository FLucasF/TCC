package br.com.marealta;

import br.com.marealta.plan.RatePlan;
import br.com.marealta.plan.RatePlans;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingTest {
    private static final BigDecimal STD = Money.of("249.90");
    private static final BigDecimal DLX = Money.of("389.90");

    private RatePlan.Price price(String plan, RoomType t, BigDecimal rate, String in, String out, String company) {
        LocalDate i = LocalDate.parse(in), o = LocalDate.parse(out);
        int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(i, o);
        RatePlan p = RatePlans.get(plan);
        RatePlan.Booking b = new RatePlan.Booking(t, i, o, nights, company);
        p.validate(b);
        return Pricing.price(p, b, rate);
    }

    @Test
    void highSeasonBoundaries() {
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-12-14")));
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2027-12-15")));
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2028-02-29")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-03-01")));
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2027-07-31")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-08-01")));
    }

    @Test
    void standardHighSeason() {
        var p = price("STANDARD", RoomType.STANDARD, STD, "2027-07-10", "2027-07-12", null);
        assertEquals(Money.of("599.76"), p.total());
    }

    @Test
    void corporate() {
        var p = price("CORPORATE", RoomType.DELUXE, DLX, "2027-03-10", "2027-03-12", "TECNOSUL");
        assertEquals("779.80", p.subtotal().toPlainString());
        assertEquals("93.58", p.discount().toPlainString());
        assertEquals("686.22", p.total().toPlainString());
    }

    @Test
    void corporateRequiresKnownCompany() {
        assertThrows(ApiException.class, () -> price("CORPORATE", RoomType.DELUXE, DLX, "2027-03-10", "2027-03-12", null));
        assertThrows(ApiException.class, () -> price("CORPORATE", RoomType.DELUXE, DLX, "2027-03-10", "2027-03-12", "ACME"));
    }

    @Test
    void weekendPackage() {
        var p = price("WEEKEND_PACKAGE", RoomType.DELUXE, DLX, "2027-03-12", "2027-03-14", null);
        assertEquals("820.00", p.total().toPlainString());
        assertEquals("410.00", RatePlans.get("WEEKEND_PACKAGE").refund(p.total()).toPlainString());
        // preço fechado mesmo na alta temporada
        assertEquals("520.00", price("WEEKEND_PACKAGE", RoomType.STANDARD, STD, "2027-07-09", "2027-07-11", null).total().toPlainString());
        assertThrows(ApiException.class, () -> price("WEEKEND_PACKAGE", RoomType.DELUXE, DLX, "2027-03-13", "2027-03-15", null));
        assertThrows(ApiException.class, () -> price("WEEKEND_PACKAGE", RoomType.DELUXE, DLX, "2027-03-12", "2027-03-15", null));
    }

    @Test
    void longStay() {
        var p = price("LONG_STAY", RoomType.STANDARD, STD, "2027-03-01", "2027-03-08", null);
        assertEquals("1749.30", p.subtotal().toPlainString());
        assertEquals("174.93", p.discount().toPlainString());
        assertEquals("1574.37", p.total().toPlainString());
        assertThrows(ApiException.class, () -> price("LONG_STAY", RoomType.STANDARD, STD, "2027-03-01", "2027-03-07", null));
    }

    @Test
    void refunds() {
        BigDecimal paid = Money.of("100.00");
        assertEquals(paid, RatePlans.get("STANDARD").refund(paid));
        assertEquals(Money.ZERO, RatePlans.get("NON_REFUNDABLE").refund(paid));
        assertEquals(paid, RatePlans.get("CORPORATE").refund(paid));
        assertEquals(paid, RatePlans.get("LONG_STAY").refund(paid));
    }

    @Test
    void formatting() {
        assertEquals("R$ 1.234,56", Money.brl(Money.of("1234.56")));
        assertEquals("R$ 0,00", Money.brl(Money.ZERO));
        assertEquals(Money.of("0.12"), Money.round(new BigDecimal("0.125")));
    }
}
