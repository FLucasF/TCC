package br.com.marealta.pousada;

import br.com.marealta.pousada.domain.*;
import br.com.marealta.pousada.notification.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainTest {

    private static Reservation reservation(Room room, String in, String out, RatePlan plan) {
        Quote q = Quote.of(room, LocalDate.parse(in), LocalDate.parse(out), plan);
        return new Reservation(7, room.number(), "Carla Dias", "carla@x.com", "111.222.333-44",
                LocalDate.parse(in), LocalDate.parse(out), 2, plan, q.nights(), q.subtotal(), q.discount(),
                q.total(), ReservationStatus.PENDING, Money.ZERO, null);
    }

    @Test
    void highSeasonPricing() {
        Quote q = Quote.of(Room.of(101, RoomType.STANDARD), LocalDate.parse("2027-07-10"),
                LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), q.total());
        assertTrue(Season.isHigh(LocalDate.parse("2026-12-15")));
        assertFalse(Season.isHigh(LocalDate.parse("2026-12-14")));
        assertTrue(Season.isHigh(LocalDate.parse("2027-02-28")));
        assertFalse(Season.isHigh(LocalDate.parse("2027-03-01")));
    }

    @Test
    void nonRefundableDiscountUsesHalfEven() {
        Quote q = Quote.of(Room.of(101, RoomType.STANDARD), LocalDate.parse("2027-03-01"),
                LocalDate.parse("2027-03-02"), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("37.48"), q.discount()); // 37.485 -> par (8)
        assertEquals(new BigDecimal("212.42"), q.total());
    }

    @Test
    void receiptText() {
        Reservation r = reservation(Room.of(201, RoomType.DELUXE), "2027-03-10", "2027-03-12", RatePlan.STANDARD);
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Receipt.of(r, Room.of(201, RoomType.DELUXE)).render());
        assertEquals("R$ 1.234,56", Money.format(new BigDecimal("1234.56")));
    }

    @Test
    void cancellationRefundFollowsPlan() {
        Room room = Room.of(101, RoomType.STANDARD);
        Reservation std = reservation(room, "2027-03-10", "2027-03-12", RatePlan.STANDARD).confirm().cancel();
        Reservation nr = reservation(room, "2027-03-10", "2027-03-12", RatePlan.NON_REFUNDABLE).confirm().cancel();
        Reservation pending = reservation(room, "2027-03-10", "2027-03-12", RatePlan.STANDARD).cancel();
        assertEquals(std.total(), std.refund());
        assertEquals(Money.ZERO, nr.refund());
        assertEquals(nr.total(), nr.retained());
        assertEquals(Money.ZERO, pending.refund());
        assertThrows(NotAllowedException.class, () -> pending.confirm());
    }

    @Test
    void confirmationNotifiesGuestHousekeepingAndFinance() {
        Room room = Room.of(201, RoomType.DELUXE);
        Reservation r = reservation(room, "2027-03-10", "2027-03-12", RatePlan.STANDARD).confirm();
        ReservationEvents events = new ReservationEvents(
                List.of(new GuestNotifier(), new HousekeepingNotifier(), new FinanceNotifier()));
        List<Message> sent = events.confirmed(r, room);
        assertEquals(3, sent.size());
        assertTrue(sent.stream().anyMatch(m -> m.to().equals("carla@x.com")
                && m.subject().equals("Reserva confirmada - Pousada Maré Alta")
                && m.body().contains("Comprovante de reserva nº 7")));
        assertTrue(sent.stream().anyMatch(m -> m.subject().equals("Reserva confirmada - quarto 201")));
        assertTrue(sent.stream().anyMatch(m -> m.subject().equals("Pagamento recebido - reserva 7")
                && m.body().contains("R$ 779,80")));
    }

    @Test
    void cancellationOfPaidReservationSkipsFinanceWhenNothingToRefund() {
        Room room = Room.of(201, RoomType.DELUXE);
        ReservationEvents events = new ReservationEvents(
                List.of(new GuestNotifier(), new HousekeepingNotifier(), new FinanceNotifier()));
        Reservation refundable = reservation(room, "2027-03-10", "2027-03-12", RatePlan.STANDARD).confirm().cancel();
        Reservation nonRefundable = reservation(room, "2027-03-10", "2027-03-12", RatePlan.NON_REFUNDABLE)
                .confirm().cancel();
        assertEquals(java.util.Set.of("Reserva cancelada - quarto 201", "Reembolso - reserva 7"),
                events.paidReservationCancelled(refundable, room).stream().map(Message::subject)
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(List.of("Reserva cancelada - quarto 201"),
                events.paidReservationCancelled(nonRefundable, room).stream().map(Message::subject).toList());
    }
}
