package br.com.marealta;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationsTest {
    private final Rooms rooms = new Rooms();
    private final Reservation pending = new Reservation(7, 201, "Carla Dias", "carla@example.com",
            "111.222.333-44", LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2,
            RatePlan.STANDARD, 2, Money.of("779.80"), Money.ZERO, Money.of("779.80"),
            ReservationStatus.PENDING, Money.ZERO, null);

    @Test
    void guestEmailEmbedsTheReceipt() {
        Message m = new GuestNotifier(rooms).confirmed(pending.confirm()).get(0);
        assertEquals("carla@example.com", m.to());
        assertEquals("Reserva confirmada - Pousada Maré Alta", m.subject());
        assertEquals("""
                Olá, Carla Dias!

                Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:

                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80

                A entrada é a partir das 14h, e a saída, até as 12h.
                Até breve!""", m.body());
    }

    @Test
    void receiptShowsDiscountAndSingularNight() {
        Reservation r = new Reservation(1, 101, "A", "a@b", "1", LocalDate.parse("2027-03-10"),
                LocalDate.parse("2027-03-11"), 1, RatePlan.NON_REFUNDABLE, 1, Money.of("1234.56"),
                Money.of("185.18"), Money.of("1049.38"), ReservationStatus.CONFIRMED, Money.of("1049.38"), null);
        String text = Receipt.of(r, rooms.get(101)).toText();
        assertTrue(text.contains("| 1 diária\n"), text);
        assertTrue(text.contains("Diárias: R$ 1.234,56\nDesconto: R$ 185,18\nTotal: R$ 1.049,38"), text);
    }

    @Test
    void housekeepingAndFinanceOnConfirmation() {
        Reservation paid = pending.confirm();
        Message h = new HousekeepingNotifier("governanca@x").confirmed(paid).get(0);
        assertEquals("Reserva confirmada - quarto 201", h.subject());
        assertTrue(h.body().contains("10/03/2027") && h.body().contains("12/03/2027") && h.body().contains("2"));
        Message f = new FinanceNotifier("financeiro@x").confirmed(paid).get(0);
        assertEquals("Pagamento recebido - reserva 7", f.subject());
        assertTrue(f.body().contains("R$ 779,80"));
    }

    @Test
    void financeIsOnlyToldOfRefundsWhenThereIsOne() {
        FinanceNotifier finance = new FinanceNotifier("financeiro@x");
        Message m = finance.cancelledAfterConfirmation(pending.confirm().cancel()).get(0);
        assertEquals("Reembolso - reserva 7", m.subject());
        assertTrue(m.body().contains("R$ 779,80"));

        Reservation nonRefundable = new Reservation(8, 201, "A", "a@b", "1", pending.checkIn(), pending.checkOut(),
                2, RatePlan.NON_REFUNDABLE, 2, Money.of("779.80"), Money.of("116.97"), Money.of("662.83"),
                ReservationStatus.PENDING, Money.ZERO, null);
        assertEquals(List.of(), finance.cancelledAfterConfirmation(nonRefundable.confirm().cancel()));
        Message h = new HousekeepingNotifier("g@x").cancelledAfterConfirmation(nonRefundable.confirm().cancel()).get(0);
        assertEquals("Reserva cancelada - quarto 201", h.subject());
    }

    @Test
    void invalidTransitionsAreRejected() {
        assertThrows(DomainException.class, pending::checkInGuest);
        assertThrows(DomainException.class, pending::checkOutGuest);
        assertThrows(DomainException.class, () -> pending.confirm().confirm());
        assertThrows(DomainException.class, () -> pending.confirm().checkInGuest().cancel());
        assertEquals(Money.ZERO, pending.cancel().refund());
    }
}
