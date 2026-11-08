package br.com.marealta;

import br.com.marealta.plan.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PlansTest {
    static final Room STANDARD = new Room(101, RoomType.STANDARD, 2, Money.of("249.90"));
    static final Room DELUXE = new Room(201, RoomType.DELUXE, 3, Money.of("389.90"));

    static Quote quote(String plan, Room room, String in, String out, String company) {
        return RatePlans.find(plan).orElseThrow()
                .quote(new PricingContext(room, LocalDate.parse(in), LocalDate.parse(out), company));
    }

    @Test void highSeasonStandard() {
        Quote q = quote("STANDARD", STANDARD, "2027-07-10", "2027-07-12", null);
        assertEquals(Money.of("599.76"), q.total());
    }

    @Test void seasonBoundaries() {
        assertFalse(PricingContext.isHighSeason(LocalDate.parse("2027-12-14")));
        assertTrue(PricingContext.isHighSeason(LocalDate.parse("2027-12-15")));
        assertTrue(PricingContext.isHighSeason(LocalDate.parse("2028-02-29")));
        assertFalse(PricingContext.isHighSeason(LocalDate.parse("2027-03-01")));
    }

    @Test void nonRefundable() {
        Quote q = quote("NON_REFUNDABLE", DELUXE, "2027-03-10", "2027-03-12", null);
        assertEquals(Money.of("779.80"), q.subtotal());
        assertEquals(Money.of("116.97"), q.discount());
        assertEquals(Money.ZERO, RatePlans.find("NON_REFUNDABLE").orElseThrow().refund(q.total()));
    }

    @Test void corporate() {
        Quote q = quote("CORPORATE", DELUXE, "2027-03-10", "2027-03-12", "TECNOSUL");
        assertEquals(Money.of("93.58"), q.discount());
        assertEquals(Money.of("686.22"), q.total());
        assertThrows(ApiException.class, () -> quote("CORPORATE", DELUXE, "2027-03-10", "2027-03-12", null));
        assertThrows(ApiException.class, () -> quote("CORPORATE", DELUXE, "2027-03-10", "2027-03-12", "OUTRA"));
    }

    @Test void weekendPackage() {
        Quote q = quote("WEEKEND_PACKAGE", DELUXE, "2027-03-12", "2027-03-14", null);
        assertEquals(Money.of("820.00"), q.total());
        assertEquals(Money.ZERO, q.discount());
        assertEquals(Money.of("410.00"), RatePlans.find("WEEKEND_PACKAGE").orElseThrow().refund(q.total()));
        assertEquals(Money.of("520.00"), quote("WEEKEND_PACKAGE", STANDARD, "2027-07-09", "2027-07-11", null).total());
        assertThrows(ApiException.class, () -> quote("WEEKEND_PACKAGE", DELUXE, "2027-03-13", "2027-03-15", null));
        assertThrows(ApiException.class, () -> quote("WEEKEND_PACKAGE", DELUXE, "2027-03-12", "2027-03-15", null));
    }

    @Test void longStay() {
        Quote q = quote("LONG_STAY", STANDARD, "2027-03-01", "2027-03-08", null);
        assertEquals(Money.of("1749.30"), q.subtotal());
        assertEquals(Money.of("174.93"), q.discount());
        assertEquals(Money.of("1574.37"), q.total());
        assertThrows(ApiException.class, () -> quote("LONG_STAY", STANDARD, "2027-03-01", "2027-03-07", null));
    }

    @Test void halfEvenRounding() {
        assertEquals(Money.of("0.12"), Money.times(new BigDecimal("0.25"), new BigDecimal("0.5")));
    }

    @Test void receipt() {
        Reservation r = new Reservation(7, 201, "Carla Dias", "c@x.com", "111.222.333-44", LocalDate.parse("2027-03-10"),
                LocalDate.parse("2027-03-12"), 2, "STANDARD", null, 2, Money.of("779.80"), Money.ZERO, Money.of("779.80"),
                ReservationStatus.CONFIRMED, Money.of("779.80"), null);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Receipt.text(r, DELUXE));
        assertEquals("R$ 1.234,56", Money.brl(Money.of("1234.56")));
    }
}
