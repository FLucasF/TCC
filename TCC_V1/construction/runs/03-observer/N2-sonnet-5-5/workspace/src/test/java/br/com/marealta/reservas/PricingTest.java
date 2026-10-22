package br.com.marealta.reservas;

import br.com.marealta.reservas.domain.Money;
import br.com.marealta.reservas.domain.Quote;
import br.com.marealta.reservas.domain.RatePlan;
import br.com.marealta.reservas.domain.Receipt;
import br.com.marealta.reservas.domain.Room;
import br.com.marealta.reservas.domain.RoomType;
import br.com.marealta.reservas.domain.Season;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingTest {
    private static final Room STANDARD = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
    private static final Room DELUXE = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));

    @Test
    void highSeasonNightsCostTwentyPercentMore() {
        Quote q = Quote.of(STANDARD, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(new BigDecimal("599.76"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Season.isHigh(LocalDate.of(2027, 12, 14)));
        assertTrue(Season.isHigh(LocalDate.of(2027, 12, 15)));
        assertTrue(Season.isHigh(LocalDate.of(2028, 2, 29)));
        assertFalse(Season.isHigh(LocalDate.of(2027, 3, 1)));
        assertFalse(Season.isHigh(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void stayCrossingIntoHighSeasonPricesEachNight() {
        Quote q = Quote.of(STANDARD, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD);
        assertEquals(new BigDecimal("549.78"), q.total()); // 249.90 + 299.88
    }

    @Test
    void nonRefundableDiscountRoundsHalfEven() {
        Quote q = Quote.of(DELUXE, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), q.subtotal());
        assertEquals(new BigDecimal("116.97"), q.discount());
        assertEquals(new BigDecimal("662.83"), q.total());
    }

    @Test
    void receiptText() {
        Quote q = Quote.of(DELUXE, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.STANDARD);
        Receipt receipt = new Receipt(7, "Carla Dias", "111.222.333-44", 201, RoomType.DELUXE,
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), q.nights(), q.subtotal(), q.discount(),
                q.total());
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", receipt.toText());
    }

    @Test
    void moneyFormat() {
        assertEquals("R$ 1.234,56", Money.brl(new BigDecimal("1234.56")));
        assertEquals("R$ 0,00", Money.brl(Money.ZERO));
    }
}
