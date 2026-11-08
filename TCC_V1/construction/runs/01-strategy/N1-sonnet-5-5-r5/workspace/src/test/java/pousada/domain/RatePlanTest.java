package pousada.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RatePlanTest {

    private static Quote quote(String plan, int room, String in, String out, String company) {
        Booking b = new Booking(Rooms.find(room).orElseThrow(), LocalDate.parse(in), LocalDate.parse(out), 2, company);
        return RatePlans.get(plan).quote(b);
    }

    private static void assertQuote(Quote q, String subtotal, String discount, String total) {
        assertEquals(Money.of(subtotal), q.subtotal());
        assertEquals(Money.of(discount), q.discount());
        assertEquals(Money.of(total), q.total());
    }

    @Test
    void standardInHighSeason() {
        assertQuote(quote("STANDARD", 101, "2027-07-10", "2027-07-12", null), "599.76", "0", "599.76");
    }

    @Test
    void nonRefundableDiscount() {
        assertQuote(quote("NON_REFUNDABLE", 201, "2027-03-10", "2027-03-12", null), "779.80", "116.97", "662.83");
    }

    @Test
    void corporateTecnosul() {
        assertQuote(quote("CORPORATE", 201, "2027-03-10", "2027-03-12", "TECNOSUL"), "779.80", "93.58", "686.22");
    }

    @Test
    void corporateRequiresKnownCompany() {
        assertEquals("INVALID_REQUEST", assertThrows(DomainException.class,
                () -> quote("CORPORATE", 201, "2027-03-10", "2027-03-12", null)).code());
        assertThrows(DomainException.class, () -> quote("CORPORATE", 201, "2027-03-10", "2027-03-12", "OUTRA"));
    }

    @Test
    void weekendPackage() {
        assertQuote(quote("WEEKEND_PACKAGE", 201, "2027-03-12", "2027-03-14", null), "820.00", "0", "820.00");
        assertQuote(quote("WEEKEND_PACKAGE", 301, "2027-07-09", "2027-07-11", null), "1200.00", "0", "1200.00");
        assertEquals(Money.of("410.00"), RatePlans.get("WEEKEND_PACKAGE").refund(Money.of("820.00")));
    }

    @Test
    void weekendPackageOnlyFridayToSunday() {
        assertThrows(DomainException.class, () -> quote("WEEKEND_PACKAGE", 201, "2027-03-11", "2027-03-13", null));
        assertThrows(DomainException.class, () -> quote("WEEKEND_PACKAGE", 201, "2027-03-12", "2027-03-15", null));
    }

    @Test
    void longStay() {
        assertQuote(quote("LONG_STAY", 101, "2027-03-01", "2027-03-08", null), "1749.30", "174.93", "1574.37");
        assertThrows(DomainException.class, () -> quote("LONG_STAY", 101, "2027-03-01", "2027-03-07", null));
    }

    @Test
    void refunds() {
        assertEquals(Money.of("100"), RatePlans.get("STANDARD").refund(Money.of("100")));
        assertEquals(Money.ZERO, RatePlans.get("NON_REFUNDABLE").refund(Money.of("100")));
        assertEquals(Money.of("100"), RatePlans.get("CORPORATE").refund(Money.of("100")));
        assertEquals(Money.of("100"), RatePlans.get("LONG_STAY").refund(Money.of("100")));
    }

    @Test
    void highSeasonBoundaries() {
        assertFalse(Season.isHigh(LocalDate.parse("2027-12-14")));
        assertTrue(Season.isHigh(LocalDate.parse("2027-12-15")));
        assertTrue(Season.isHigh(LocalDate.parse("2028-02-29")));
        assertFalse(Season.isHigh(LocalDate.parse("2027-03-01")));
        assertTrue(Season.isHigh(LocalDate.parse("2027-07-31")));
    }
}
