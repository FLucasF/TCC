package br.com.marealta;

import br.com.marealta.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingAndReceiptTest {
    private final Room standard = RoomCatalog.find(101).orElseThrow();
    private final Room deluxe = RoomCatalog.find(201).orElseThrow();

    @Test
    void highSeasonNightsCost20PercentMore() {
        var q = Pricing.quote(standard, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(new BigDecimal("599.76"), q.subtotal());
        assertEquals(new BigDecimal("599.76"), q.total());
    }

    @Test
    void seasonBoundaries() {
        var q = Pricing.quote(standard, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD);
        assertEquals(new BigDecimal("549.78"), q.subtotal()); // 249.90 + 299.88
        q = Pricing.quote(standard, LocalDate.of(2028, 2, 28), LocalDate.of(2028, 3, 2), RatePlan.STANDARD);
        assertEquals(new BigDecimal("849.66"), q.subtotal()); // 299.88 x2 (fev) + 249.90 (mar)
    }

    @Test
    void nonRefundableDiscountUsesHalfEven() {
        var q = Pricing.quote(deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), q.subtotal());
        assertEquals(new BigDecimal("116.97"), q.discount());
        assertEquals(new BigDecimal("662.83"), q.total());
    }

    @Test
    void receiptText() {
        var q = Pricing.quote(deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.STANDARD);
        var r = new Reservation(7, 201, "Carla Dias", "c@x.com", "111.222.333-44", LocalDate.of(2027, 3, 10),
                LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD, q, ReservationStatus.CONFIRMED, q.total(), null);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Receipt.of(r, deluxe).text());
        assertEquals("R$ 1.234,56", Money.format(new BigDecimal("1234.56")));
    }

    @Test
    void cancellationRefunds() {
        var q = Pricing.quote(standard, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE);
        var r = new Reservation(1, 101, "A", "a@b.com", "1", LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), 1,
                RatePlan.NON_REFUNDABLE, q, ReservationStatus.PENDING, Money.ZERO, null);
        r.confirm();
        r.cancel();
        assertEquals(Money.ZERO, r.refund());
        assertEquals(q.total(), r.retainedAmount());
        assertThrows(ApiException.class, r::checkIn);
    }
}
