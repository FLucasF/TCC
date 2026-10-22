package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.marealta.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DomainTest {
    private static final Room STANDARD = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
    private static final Room DELUXE = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));

    @Test
    void highSeasonNightsCost20PercentMore() {
        Quote q = Quote.of(STANDARD, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(new BigDecimal("599.76"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertEquals(false, HighSeason.contains(LocalDate.of(2027, 12, 14)));
        assertEquals(true, HighSeason.contains(LocalDate.of(2027, 12, 15)));
        assertEquals(true, HighSeason.contains(LocalDate.of(2028, 2, 29)));
        assertEquals(false, HighSeason.contains(LocalDate.of(2027, 3, 1)));
    }

    @Test
    void nonRefundableGivesDiscount() {
        Quote q = Quote.of(STANDARD, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 3), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("499.80"), q.subtotal());
        assertEquals(new BigDecimal("74.97"), q.discount());
        assertEquals(new BigDecimal("424.83"), q.total());
    }

    @Test
    void receiptText() {
        Reservation r = new Reservation(7, 201, "Carla Dias", "c@x.com", "111.222.333-44",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, RatePlan.STANDARD, 2,
                new BigDecimal("779.80"), Money.ZERO, new BigDecimal("779.80"), ReservationStatus.CONFIRMED,
                new BigDecimal("779.80"), null);
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
        assertEquals("R$ 1.234,56", Money.brl(new BigDecimal("1234.56")));
    }

    @Test
    void cancellationRefunds() {
        Reservation pending = new Reservation(1, 101, "A", "a@b.c", "1", LocalDate.of(2027, 3, 1),
                LocalDate.of(2027, 3, 2), 1, RatePlan.NON_REFUNDABLE, 1, new BigDecimal("249.90"),
                new BigDecimal("37.49"), new BigDecimal("212.41"), ReservationStatus.PENDING, Money.ZERO, null);
        assertEquals(Money.ZERO, pending.cancelled().refund());
        assertEquals(Money.ZERO, pending.confirmed().cancelled().refund());
        assertEquals(new BigDecimal("212.41"), pending.confirmed().cancelled().revenue());
        assertThrows(DomainException.class, () -> pending.checkedIn());
        assertThrows(DomainException.class, () -> pending.cancelled().cancelled());
    }
}
