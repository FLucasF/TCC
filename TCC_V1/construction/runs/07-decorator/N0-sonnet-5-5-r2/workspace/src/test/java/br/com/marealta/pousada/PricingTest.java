package br.com.marealta.pousada;

import br.com.marealta.pousada.Domain.RatePlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingTest {
    private static final BigDecimal STD = new BigDecimal("249.90");

    @Test
    void highSeasonExample() {
        var q = Pricing.quote(STD, LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(new BigDecimal("599.76"), q.subtotal());
        assertEquals(new BigDecimal("0.00"), q.discount());
        assertEquals(new BigDecimal("599.76"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-12-14")));
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2027-12-15")));
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2028-02-29")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-03-01")));
        assertTrue(Pricing.isHighSeason(LocalDate.parse("2027-07-31")));
        assertFalse(Pricing.isHighSeason(LocalDate.parse("2027-08-01")));
    }

    @Test
    void nonRefundableDiscount() {
        var q = Pricing.quote(new BigDecimal("389.90"), LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"),
                RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), q.subtotal());
        assertEquals(new BigDecimal("116.97"), q.discount()); // 116.97 exato
        assertEquals(new BigDecimal("662.83"), q.total());
    }

    @Test
    void halfEvenRounding() {
        // 249.90 * 0.15 = 37.485 -> meio para o par = 37.48
        var q = Pricing.quote(STD, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("37.48"), q.discount());
        assertEquals(new BigDecimal("212.42"), q.total());
    }

    @Test
    void receiptText() {
        var r = new Receipt(7, "Carla Dias", "11122233344", 201, "DELUXE", LocalDate.parse("2027-03-10"),
                LocalDate.parse("2027-03-12"), 2, new BigDecimal("1779.80"), new BigDecimal("0.00"), new BigDecimal("1779.80"));
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 1.779,80
                Total: R$ 1.779,80""", r.text());
    }
}
