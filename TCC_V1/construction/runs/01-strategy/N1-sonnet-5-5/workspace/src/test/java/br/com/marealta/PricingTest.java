package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.marealta.domain.ApiException;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Pricing;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.Stay;
import br.com.marealta.plan.RatePlans;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingTest {

    private static Pricing quote(String plan, int room, String in, String out, String company) {
        Stay stay = new Stay(Room.find(room).orElseThrow(), LocalDate.parse(in), LocalDate.parse(out), company);
        return RatePlans.get(plan).quote(stay);
    }

    private static void assertPricing(String subtotal, String discount, String total, Pricing p) {
        assertEquals(Money.of(subtotal), p.subtotal());
        assertEquals(Money.of(discount), p.discount());
        assertEquals(Money.of(total), p.total());
    }

    @Test
    void highSeasonNightsAre20PercentHigher() {
        assertPricing("599.76", "0", "599.76", quote("STANDARD", 101, "2027-07-10", "2027-07-12", null));
    }

    @Test
    void seasonStartsOnDecember15() {
        assertPricing("549.78", "0", "549.78", quote("STANDARD", 101, "2027-12-14", "2027-12-16", null));
    }

    @Test
    void nonRefundableGives15PercentOffTheSum() {
        assertPricing("499.80", "74.97", "424.83", quote("NON_REFUNDABLE", 101, "2027-03-10", "2027-03-12", null));
    }

    @Test
    void corporateUsesCompanyDiscount() {
        assertPricing("779.80", "93.58", "686.22", quote("CORPORATE", 201, "2027-03-10", "2027-03-12", "TECNOSUL"));
    }

    @Test
    void corporateNeedsKnownCompany() {
        assertThrows(ApiException.class, () -> quote("CORPORATE", 201, "2027-03-10", "2027-03-12", null));
        assertThrows(ApiException.class, () -> quote("CORPORATE", 201, "2027-03-10", "2027-03-12", "OUTRA"));
    }

    @Test
    void weekendPackageHasClosedPrice() {
        assertPricing("820.00", "0", "820.00", quote("WEEKEND_PACKAGE", 201, "2027-03-12", "2027-03-14", null));
        assertPricing("1200.00", "0", "1200.00", quote("WEEKEND_PACKAGE", 301, "2027-07-09", "2027-07-11", null));
    }

    @Test
    void weekendPackageOnlyFridayToSunday() {
        assertThrows(ApiException.class, () -> quote("WEEKEND_PACKAGE", 201, "2027-03-13", "2027-03-15", null));
        assertThrows(ApiException.class, () -> quote("WEEKEND_PACKAGE", 201, "2027-03-12", "2027-03-15", null));
    }

    @Test
    void weekendPackageRefundsHalf() {
        assertEquals(Money.of("410.00"), RatePlans.get("WEEKEND_PACKAGE").refund(Money.of("820.00")));
    }

    @Test
    void longStayNeedsSevenNights() {
        assertPricing("1749.30", "174.93", "1574.37", quote("LONG_STAY", 101, "2027-03-01", "2027-03-08", null));
        assertThrows(ApiException.class, () -> quote("LONG_STAY", 101, "2027-03-01", "2027-03-07", null));
    }

    @Test
    void refundRules() {
        Money.of("1");
        assertEquals(Money.of("100.00"), RatePlans.get("STANDARD").refund(Money.of("100.00")));
        assertEquals(Money.of("0.00"), RatePlans.get("NON_REFUNDABLE").refund(Money.of("100.00")));
        assertEquals(Money.of("100.00"), RatePlans.get("CORPORATE").refund(Money.of("100.00")));
        assertEquals(Money.of("100.00"), RatePlans.get("LONG_STAY").refund(Money.of("100.00")));
    }

    @Test
    void formatsMoneyInReais() {
        assertEquals("R$ 1.234,56", Money.brl(Money.of("1234.56")));
        assertEquals("R$ 0,50", Money.brl(Money.of("0.5")));
    }
}
