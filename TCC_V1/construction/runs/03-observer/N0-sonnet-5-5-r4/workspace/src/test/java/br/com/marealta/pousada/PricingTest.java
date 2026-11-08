package br.com.marealta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Pricing;
import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingTest {
    private static final Room STANDARD = Room.ALL.get(0);
    private static final Room DELUXE = Room.ALL.get(2);

    @Test
    void highSeasonNightsCostTwentyPercentMore() {
        Pricing p = Pricing.quote(STANDARD, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), p.subtotal());
        assertEquals(new BigDecimal("599.76"), p.total());
        assertEquals(2, p.nights());
    }

    @Test
    void seasonBoundaries() {
        // 14/12 normal, 15/12 alta; 28/02 alta, 01/03 normal
        Pricing p = Pricing.quote(STANDARD, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD);
        assertEquals(new BigDecimal("549.78"), p.subtotal());
        Pricing q = Pricing.quote(STANDARD, LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 2), RatePlan.STANDARD);
        assertEquals(new BigDecimal("549.78"), q.subtotal());
    }

    @Test
    void nonRefundableDiscountRoundsHalfEven() {
        Pricing p = Pricing.quote(DELUXE, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), p.subtotal());
        assertEquals(new BigDecimal("116.97"), p.discount());
        assertEquals(new BigDecimal("662.83"), p.total());
        // 0.15 * 249.90 = 37.485 -> 37.48 (meio para o par)
        Pricing one = Pricing.quote(STANDARD, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("37.48"), one.discount());
    }

    @Test
    void receiptText() {
        Reservation r = Reservation.pending(DELUXE, "Carla Dias", "c@x.com", "111.222.333-44",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD).withId(7);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Receipt.of(r, DELUXE).toText());
    }

    @Test
    void moneyFormat() {
        assertEquals("R$ 1.234,56", Money.format(new BigDecimal("1234.56")));
        assertEquals("R$ 0,00", Money.format(Money.ZERO));
    }
}
