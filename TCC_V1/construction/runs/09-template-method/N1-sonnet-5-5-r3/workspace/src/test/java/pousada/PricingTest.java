package pousada;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingTest {
    private final Room standard = new Room(101, RoomType.STANDARD, 2, Money.of("249.90"));

    @Test
    void highSeasonExample() {
        Pricing.Quote q = Pricing.quote(standard, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(Money.of("599.76"), q.subtotal());
        assertEquals(Money.of("0.00"), q.discount());
        assertEquals(Money.of("599.76"), q.total());
    }

    @Test
    void nonRefundableDiscount() {
        Pricing.Quote q = Pricing.quote(standard, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 3), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("499.80"), q.subtotal());
        assertEquals(Money.of("74.97"), q.discount());
        assertEquals(Money.of("424.83"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Pricing.isHighSeason(LocalDate.of(2026, 12, 14)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2026, 12, 15)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 2, 28)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void roundsHalfToEven() {
        assertEquals(Money.of("0.12"), Money.round(new java.math.BigDecimal("0.125")));
        assertEquals(Money.of("0.14"), Money.round(new java.math.BigDecimal("0.135")));
    }

    @Test
    void receiptText() {
        Room deluxe = new Room(201, RoomType.DELUXE, 3, Money.of("389.90"));
        Reservation r = new Reservation(7L, 201, "Carla Dias", "c@x.com", "111.222.333-44",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD, 2,
                Money.of("779.80"), Money.ZERO, Money.of("779.80"), ReservationStatus.CONFIRMED,
                Money.of("779.80"), null);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Receipt.of(r, deluxe).toText());
        assertEquals("R$ 1.234,56", Money.brl(Money.of("1234.56")));
    }
}
