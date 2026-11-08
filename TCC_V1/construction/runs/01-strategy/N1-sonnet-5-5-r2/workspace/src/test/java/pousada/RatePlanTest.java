package pousada;

import org.junit.jupiter.api.Test;
import pousada.domain.AppException;
import pousada.domain.Money;
import pousada.domain.RoomCatalog;
import pousada.plan.Pricing;
import pousada.plan.RatePlans;
import pousada.plan.Stay;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RatePlanTest {
    private Pricing quote(String plan, int room, String in, String out, String company) {
        Stay stay = new Stay(RoomCatalog.find(room).orElseThrow(), LocalDate.parse(in), LocalDate.parse(out), company);
        return RatePlans.byCode(plan).quote(stay);
    }

    private void assertPricing(Pricing p, String subtotal, String discount, String total) {
        assertEquals(Money.of(subtotal), p.subtotal());
        assertEquals(Money.of(discount), p.discount());
        assertEquals(Money.of(total), p.total());
    }

    @Test
    void highSeasonStandard() {
        assertPricing(quote("STANDARD", 101, "2027-07-10", "2027-07-12", null), "599.76", "0", "599.76");
    }

    @Test
    void corporate() {
        assertPricing(quote("CORPORATE", 201, "2027-03-10", "2027-03-12", "TECNOSUL"), "779.80", "93.58", "686.22");
        assertThrows(AppException.class, () -> quote("CORPORATE", 201, "2027-03-10", "2027-03-12", null));
        assertThrows(AppException.class, () -> quote("CORPORATE", 201, "2027-03-10", "2027-03-12", "OUTRA"));
    }

    @Test
    void weekendPackage() {
        assertPricing(quote("WEEKEND_PACKAGE", 201, "2027-03-12", "2027-03-14", null), "820.00", "0", "820.00");
        assertPricing(quote("WEEKEND_PACKAGE", 301, "2027-07-09", "2027-07-11", null), "1200.00", "0", "1200.00");
        assertEquals(Money.of("410.00"), RatePlans.byCode("WEEKEND_PACKAGE").refund(Money.of("820.00")));
        assertThrows(AppException.class, () -> quote("WEEKEND_PACKAGE", 201, "2027-03-13", "2027-03-15", null));
        assertThrows(AppException.class, () -> quote("WEEKEND_PACKAGE", 201, "2027-03-12", "2027-03-15", null));
    }

    @Test
    void longStay() {
        assertPricing(quote("LONG_STAY", 101, "2027-03-01", "2027-03-08", null), "1749.30", "174.93", "1574.37");
        assertThrows(AppException.class, () -> quote("LONG_STAY", 101, "2027-03-01", "2027-03-07", null));
    }

    @Test
    void nonRefundable() {
        assertPricing(quote("NON_REFUNDABLE", 101, "2027-03-01", "2027-03-03", null), "499.80", "74.97", "424.83");
    }
}
