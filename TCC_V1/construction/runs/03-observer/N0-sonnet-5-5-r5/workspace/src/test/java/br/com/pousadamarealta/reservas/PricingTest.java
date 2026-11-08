package br.com.pousadamarealta.reservas;

import br.com.pousadamarealta.reservas.domain.*;
import br.com.pousadamarealta.reservas.notification.Brl;
import br.com.pousadamarealta.reservas.notification.ReceiptText;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingTest {
    private static final Room STANDARD = Room.DEFAULTS.get(0);
    private static final Room DELUXE = Room.DEFAULTS.get(2);

    @Test
    void highSeasonBoundaries() {
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 2, 28)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void highSeasonExample() {
        Quote q = Pricing.quote(STANDARD, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(Money.of("599.76"), q.total());
        assertEquals(Money.ZERO, q.discount());
    }

    @Test
    void nightsAreSeasonedIndividually() {
        // noites de 14/12 (baixa) e 15/12 (alta)
        Quote q = Pricing.quote(STANDARD, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD);
        assertEquals(Money.of("549.78"), q.subtotal());
    }

    @Test
    void nonRefundableDiscountRoundsHalfEven() {
        // 249.90 * 0.15 = 37.485 -> 37.48 (meio para o par)
        Quote q = Pricing.quote(STANDARD, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 2), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("37.48"), q.discount());
        assertEquals(Money.of("212.42"), q.total());
    }

    @Test
    void receiptText() {
        Receipt r = new Receipt(Receipt.TITLE, 7, "Carla Dias", "111.222.333-44", 201, RoomType.DELUXE,
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, Money.of("779.80"), Money.ZERO, Money.of("779.80"));
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", ReceiptText.of(r));
    }

    @Test
    void receiptWithDiscountAndOneNight() {
        Receipt r = new Receipt(Receipt.TITLE, 1, "A", "11122233344", 101, RoomType.STANDARD,
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), 1, Money.of("1234.56"), Money.of("185.18"), Money.of("1049.38"));
        String text = ReceiptText.of(r);
        assertTrue(text.contains("| 1 diária\n"));
        assertTrue(text.contains("Diárias: R$ 1.234,56\nDesconto: R$ 185,18\nTotal: R$ 1.049,38"));
        assertTrue(text.contains("CPF 111.222.333-44"));
        assertEquals("R$ 0,00", Brl.money(Money.ZERO));
    }
}
