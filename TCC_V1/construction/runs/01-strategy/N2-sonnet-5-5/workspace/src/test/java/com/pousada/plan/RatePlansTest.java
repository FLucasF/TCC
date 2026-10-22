package com.pousada.plan;

import com.pousada.domain.Money;
import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import com.pousada.error.ApiException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RatePlansTest {

    private final RatePlans plans = RatePlans.defaults();
    private final Room standard = new Room(101, RoomType.STANDARD, 2, Money.of("249.90"));
    private final Room deluxe = new Room(201, RoomType.DELUXE, 3, Money.of("389.90"));

    private PriceQuote quote(String plan, Room room, String in, String out, String company) {
        return plans.get(plan).quote(new PricingRequest(room, LocalDate.parse(in), LocalDate.parse(out), company));
    }

    @Test
    void highSeasonStandard() {
        PriceQuote q = quote("STANDARD", standard, "2027-07-10", "2027-07-12", null);
        assertEquals(Money.of("599.76"), q.total());
    }

    @Test
    void corporateTecnosul() {
        PriceQuote q = quote("CORPORATE", deluxe, "2027-03-10", "2027-03-12", "TECNOSUL");
        assertEquals(Money.of("779.80"), q.subtotal());
        assertEquals(Money.of("93.58"), q.discount());
        assertEquals(Money.of("686.22"), q.total());
    }

    @Test
    void corporateRequiresKnownCompany() {
        assertThrows(ApiException.class, () -> quote("CORPORATE", deluxe, "2027-03-10", "2027-03-12", null));
        assertThrows(ApiException.class, () -> quote("CORPORATE", deluxe, "2027-03-10", "2027-03-12", "OUTRA"));
    }

    @Test
    void weekendPackage() {
        PriceQuote q = quote("WEEKEND_PACKAGE", deluxe, "2027-03-12", "2027-03-14", null);
        assertEquals(Money.of("820.00"), q.total());
        assertEquals(Money.of("410.00"), plans.get("WEEKEND_PACKAGE").refund(q.total()));
        assertThrows(ApiException.class, () -> quote("WEEKEND_PACKAGE", deluxe, "2027-03-13", "2027-03-15", null));
        assertThrows(ApiException.class, () -> quote("WEEKEND_PACKAGE", deluxe, "2027-03-12", "2027-03-15", null));
    }

    @Test
    void longStay() {
        PriceQuote q = quote("LONG_STAY", standard, "2027-03-01", "2027-03-08", null);
        assertEquals(Money.of("1749.30"), q.subtotal());
        assertEquals(Money.of("174.93"), q.discount());
        assertEquals(Money.of("1574.37"), q.total());
        assertThrows(ApiException.class, () -> quote("LONG_STAY", standard, "2027-03-01", "2027-03-07", null));
    }

    @Test
    void nonRefundableGivesNothingBack() {
        PriceQuote q = quote("NON_REFUNDABLE", standard, "2027-03-01", "2027-03-03", null);
        assertEquals(Money.of("424.83"), q.total());
        assertEquals(Money.ZERO, plans.get("NON_REFUNDABLE").refund(q.total()));
    }
}
