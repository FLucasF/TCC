package br.com.mare;

import br.com.mare.domain.Money;
import br.com.mare.domain.Rooms;
import br.com.mare.plan.Booking;
import br.com.mare.plan.Quote;
import br.com.mare.plan.RatePlans;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingTest {
    private Quote quote(String plan, int room, String in, String out, String company) {
        return RatePlans.find(plan).orElseThrow()
                .quote(new Booking(Rooms.find(room).orElseThrow(), LocalDate.parse(in), LocalDate.parse(out), company));
    }

    @Test
    void highSeasonStandard() {
        Quote q = quote("STANDARD", 101, "2027-07-10", "2027-07-12", null);
        assertEquals(Money.of("599.76"), q.total());
    }

    @Test
    void corporate() {
        Quote q = quote("CORPORATE", 201, "2027-03-10", "2027-03-12", "TECNOSUL");
        assertEquals(Money.of("779.80"), q.subtotal());
        assertEquals(Money.of("93.58"), q.discount());
        assertEquals(Money.of("686.22"), q.total());
        assertThrows(RuntimeException.class, () -> quote("CORPORATE", 201, "2027-03-10", "2027-03-12", null));
    }

    @Test
    void weekendPackage() {
        Quote q = quote("WEEKEND_PACKAGE", 201, "2027-03-12", "2027-03-14", null);
        assertEquals(Money.of("820.00"), q.total());
        assertEquals(Money.of("410.00"), RatePlans.find("WEEKEND_PACKAGE").orElseThrow().refund(q.total()));
        assertThrows(RuntimeException.class, () -> quote("WEEKEND_PACKAGE", 201, "2027-03-13", "2027-03-15", null));
    }

    @Test
    void longStay() {
        Quote q = quote("LONG_STAY", 101, "2027-03-01", "2027-03-08", null);
        assertEquals(Money.of("1749.30"), q.subtotal());
        assertEquals(Money.of("174.93"), q.discount());
        assertEquals(Money.of("1574.37"), q.total());
        assertThrows(RuntimeException.class, () -> quote("LONG_STAY", 101, "2027-03-01", "2027-03-07", null));
    }

    @Test
    void nonRefundableRefundsNothing() {
        assertEquals(Money.ZERO, RatePlans.find("NON_REFUNDABLE").orElseThrow().refund(Money.of("100")));
    }

    @Test
    void moneyFormat() {
        assertEquals("R$ 1.234,56", Money.format(Money.of("1234.56")));
        assertEquals("R$ 0,00", Money.format(Money.ZERO));
    }
}
