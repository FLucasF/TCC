package br.com.mare.pousada;

import br.com.mare.pousada.domain.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DomainTest {

    @Test
    void highSeasonNightsCost20PercentMore() {
        Quote q = Quote.of(RoomCatalog.require(101), LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(Money.of("599.76"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Season.isHigh(LocalDate.of(2027, 12, 14)));
        assertTrue(Season.isHigh(LocalDate.of(2027, 12, 15)));
        assertTrue(Season.isHigh(LocalDate.of(2028, 2, 29)));
        assertFalse(Season.isHigh(LocalDate.of(2028, 3, 1)));
        assertFalse(Season.isHigh(LocalDate.of(2027, 6, 30)));
        assertTrue(Season.isHigh(LocalDate.of(2027, 7, 31)));
        assertFalse(Season.isHigh(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void nonRefundableDiscountsSubtotal() {
        Quote q = Quote.of(RoomCatalog.require(201), LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("779.80"), q.subtotal());
        assertEquals(Money.of("116.97"), q.discount());
        assertEquals(Money.of("662.83"), q.total());
    }

    @Test
    void receiptText() {
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
                Total: R$ 779,80""", Receipt.of(r).text());
    }

    @Test
    void moneyDisplayAndTax() {
        assertEquals("R$ 1.234,56", Money.display(Money.of("1234.56")));
        Reservation r = new Reservation(1L, 101, "a", "b", "c", LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 4),
                2, RatePlan.STANDARD, 3, Money.ZERO, Money.ZERO, Money.ZERO, ReservationStatus.CONFIRMED, Money.ZERO, null);
        assertEquals(Money.of("27.00"), TourismTax.of(r));
    }

    @Test
    void cancellationRefundFollowsPlan() {
        Reservation r = new Reservation(1L, 101, "a", "b", "c", LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 4),
                2, RatePlan.NON_REFUNDABLE, 3, Money.of("100"), Money.of("15"), Money.of("85"),
                ReservationStatus.PENDING, Money.ZERO, null);
        r.confirm();
        r.cancel();
        assertEquals(Money.ZERO, r.getRefund());
        assertEquals(Money.of("85"), r.retainedAmount());
        assertThrows(BusinessException.class, r::registerCheckIn);
    }
}
