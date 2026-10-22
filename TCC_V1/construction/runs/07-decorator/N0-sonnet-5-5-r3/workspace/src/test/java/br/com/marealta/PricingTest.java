package br.com.marealta;

import static org.junit.jupiter.api.Assertions.*;

import br.com.marealta.domain.Pricing;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Receipt;
import br.com.marealta.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingTest {
    static final Room STANDARD = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
    static final Room DELUXE = new Room(201, "DELUXE", 3, new BigDecimal("389.90"));

    @Test
    void highSeasonExample() {
        var q = Pricing.quote(STANDARD, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(new BigDecimal("599.76"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2028, 2, 29)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2028, 3, 1)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void nonRefundableDiscountHalfEven() {
        // 2 noites normais de 249,90 = 499,80; 15% = 74,97
        var q = Pricing.quote(STANDARD, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 3), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("499.80"), q.subtotal());
        assertEquals(new BigDecimal("74.97"), q.discount());
        assertEquals(new BigDecimal("424.83"), q.total());
        // meio exato: 0.005 -> par
        assertEquals(new BigDecimal("0.04"), Pricing.round(new BigDecimal("0.035")));
        assertEquals(new BigDecimal("0.02"), Pricing.round(new BigDecimal("0.025")));
    }

    @Test
    void receiptText() {
        var q = Pricing.quote(DELUXE, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.STANDARD);
        var r = new Receipt(Receipt.TITLE, 7, "Carla Dias", "111.222.333-44", 201, "DELUXE",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), q.nights(), q.subtotal(), q.discount(), q.total());
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", r.toText());
        assertEquals("R$ 1.234,56", Receipt.brl(new BigDecimal("1234.56")));
    }
}
