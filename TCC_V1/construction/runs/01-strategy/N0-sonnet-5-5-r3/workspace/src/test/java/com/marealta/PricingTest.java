package com.marealta;

import static org.junit.jupiter.api.Assertions.*;

import com.marealta.plan.BookingTerms;
import com.marealta.plan.Quote;
import com.marealta.plan.RatePlans;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingTest {
    private static final Room STANDARD = new Room(101, RoomType.STANDARD, 2, RoomType.STANDARD.nightlyRate);
    private static final Room DELUXE = new Room(201, RoomType.DELUXE, 3, RoomType.DELUXE.nightlyRate);

    private static Quote quote(String plan, Room room, String in, String out, String company) {
        return RatePlans.require(plan).quote(
                new BookingTerms(room, LocalDate.parse(in), LocalDate.parse(out), company));
    }

    private static BigDecimal m(String v) { return Money.of(v); }

    @Test void highSeasonNights() {
        Quote q = quote("STANDARD", STANDARD, "2027-07-10", "2027-07-12", null);
        assertEquals(m("599.76"), q.total());
        assertEquals(2, q.nights());
    }

    @Test void seasonBoundaries() {
        assertFalse(Seasons.isHigh(LocalDate.parse("2027-12-14")));
        assertTrue(Seasons.isHigh(LocalDate.parse("2027-12-15")));
        assertTrue(Seasons.isHigh(LocalDate.parse("2028-02-29")));
        assertFalse(Seasons.isHigh(LocalDate.parse("2028-03-01")));
    }

    @Test void corporate() {
        Quote q = quote("CORPORATE", DELUXE, "2027-03-10", "2027-03-12", "TECNOSUL");
        assertEquals(m("779.80"), q.subtotal());
        assertEquals(m("93.58"), q.discount());
        assertEquals(m("686.22"), q.total());
    }

    @Test void corporateNeedsKnownCompany() {
        assertThrows(ApiException.class, () -> quote("CORPORATE", DELUXE, "2027-03-10", "2027-03-12", null));
        assertThrows(ApiException.class, () -> quote("CORPORATE", DELUXE, "2027-03-10", "2027-03-12", "ACME"));
    }

    @Test void weekendPackage() {
        Quote q = quote("WEEKEND_PACKAGE", DELUXE, "2027-03-12", "2027-03-14", null);
        assertEquals(m("820.00"), q.total());
        assertEquals(m("0.00"), q.discount());
        assertEquals(m("410.00"), RatePlans.require("WEEKEND_PACKAGE").refund(q.total()));
        assertThrows(ApiException.class, () -> quote("WEEKEND_PACKAGE", DELUXE, "2027-03-13", "2027-03-15", null));
        assertThrows(ApiException.class, () -> quote("WEEKEND_PACKAGE", DELUXE, "2027-03-12", "2027-03-15", null));
    }

    @Test void longStay() {
        Quote q = quote("LONG_STAY", STANDARD, "2027-03-01", "2027-03-08", null);
        assertEquals(m("1749.30"), q.subtotal());
        assertEquals(m("174.93"), q.discount());
        assertEquals(m("1574.37"), q.total());
        assertThrows(ApiException.class, () -> quote("LONG_STAY", STANDARD, "2027-03-01", "2027-03-07", null));
    }

    @Test void nonRefundable() {
        Quote q = quote("NON_REFUNDABLE", STANDARD, "2027-03-01", "2027-03-03", null);
        assertEquals(m("499.80"), q.subtotal());
        assertEquals(m("74.97"), q.discount());
        assertEquals(m("0.00"), RatePlans.require("NON_REFUNDABLE").refund(q.total()));
    }

    @Test void receiptText() {
        Receipt r = new Receipt(Receipt.TITLE, 7, "Carla Dias", "111.222.333-44", 201, "DELUXE",
                "2027-03-10", "2027-03-12", 2, m("779.80"), m("0.00"), m("779.80"));
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", r.text());
        assertEquals("R$ 1.234,56", Money.format(m("1234.56")));
    }
}
