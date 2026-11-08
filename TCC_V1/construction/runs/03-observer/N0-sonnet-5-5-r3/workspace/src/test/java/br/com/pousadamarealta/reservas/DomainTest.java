package br.com.pousadamarealta.reservas;

import br.com.pousadamarealta.reservas.domain.DomainException;
import br.com.pousadamarealta.reservas.domain.ErrorCode;
import br.com.pousadamarealta.reservas.domain.Money;
import br.com.pousadamarealta.reservas.domain.Pricing;
import br.com.pousadamarealta.reservas.domain.RatePlan;
import br.com.pousadamarealta.reservas.domain.Receipt;
import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.domain.Room;
import br.com.pousadamarealta.reservas.domain.RoomType;
import br.com.pousadamarealta.reservas.notification.Message;
import br.com.pousadamarealta.reservas.notification.Notifiers;
import br.com.pousadamarealta.reservas.notification.ReservationEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainTest {
    static final Room STANDARD = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
    static final Room DELUXE = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));

    static LocalDate d(String s) {
        return LocalDate.parse(s);
    }

    @Test
    void highSeasonBoundaries() {
        assertFalse(Pricing.isHighSeason(d("2027-12-14")));
        assertTrue(Pricing.isHighSeason(d("2027-12-15")));
        assertTrue(Pricing.isHighSeason(d("2028-02-29")));
        assertFalse(Pricing.isHighSeason(d("2027-03-01")));
        assertTrue(Pricing.isHighSeason(d("2027-07-01")));
        assertTrue(Pricing.isHighSeason(d("2027-07-31")));
        assertFalse(Pricing.isHighSeason(d("2027-08-01")));
    }

    @Test
    void highSeasonExample() {
        var q = Pricing.quote(STANDARD, d("2027-07-10"), d("2027-07-12"), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(new BigDecimal("599.76"), q.subtotal());
        assertEquals(new BigDecimal("0.00"), q.discount());
        assertEquals(new BigDecimal("599.76"), q.total());
    }

    @Test
    void stayCrossingSeasonChangeAndDiscount() {
        // 14/12 normal (249.90) + 15/12 alta (299.88) = 549.78; desconto 82.467 -> 82.47
        var q = Pricing.quote(STANDARD, d("2027-12-14"), d("2027-12-16"), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("549.78"), q.subtotal());
        assertEquals(new BigDecimal("82.47"), q.discount());
        assertEquals(new BigDecimal("467.31"), q.total());
    }

    @Test
    void roundsHalfToEven() {
        assertEquals(new BigDecimal("0.12"), Money.scale(new BigDecimal("0.125")));
        assertEquals(new BigDecimal("0.14"), Money.scale(new BigDecimal("0.135")));
    }

    @Test
    void formatsMoney() {
        assertEquals("R$ 1.234,56", Money.format(new BigDecimal("1234.56")));
        assertEquals("R$ 0,00", Money.format(new BigDecimal("0.00")));
        assertEquals("R$ 1.000.000,10", Money.format(new BigDecimal("1000000.10")));
    }

    Reservation reservation(Room room, long id, String in, String out, int guests, RatePlan plan) {
        Reservation r = Reservation.create(room, "Carla Dias", "carla@example.com", "111.222.333-44",
                d(in), d(out), guests, plan);
        return new Reservation(id, r.roomNumber(), r.guestName(), r.guestEmail(), r.guestCpf(), r.checkIn(),
                r.checkOut(), r.guests(), r.ratePlan(), r.nights(), r.subtotal(), r.discount(), r.total(),
                r.status(), r.amountPaid(), r.refund());
    }

    @Test
    void receiptText() {
        Reservation r = reservation(DELUXE, 7, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD).confirm();
        assertEquals("""
                Comprovante de reserva nº 7
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 12/03/2027 | 2 diárias
                Diárias: R$ 779,80
                Total: R$ 779,80""", Receipt.of(r, DELUXE).text());
    }

    @Test
    void receiptTextWithDiscountAndSingleNight() {
        Reservation r = reservation(DELUXE, 8, "2027-03-10", "2027-03-11", 2, RatePlan.NON_REFUNDABLE);
        assertEquals("""
                Comprovante de reserva nº 8
                Hóspede: Carla Dias (CPF 111.222.333-44)
                Quarto: 201 (DELUXE)
                Entrada: 10/03/2027 | Saída: 11/03/2027 | 1 diária
                Diárias: R$ 389,90
                Desconto: R$ 58,48
                Total: R$ 331,42""", Receipt.of(r, DELUXE).text());
    }

    @Test
    void stateMachineAndRefunds() {
        Reservation pending = reservation(STANDARD, 1, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        assertThrows(DomainException.class, pending::doCheckIn);
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED,
                assertThrows(DomainException.class, pending::doCheckOut).code());

        assertEquals(new BigDecimal("0.00"), pending.cancel().refund());

        Reservation confirmed = pending.confirm();
        assertEquals(confirmed.total(), confirmed.amountPaid());
        assertThrows(DomainException.class, confirmed::confirm);
        assertEquals(confirmed.total(), confirmed.cancel().refund());

        Reservation nonRefundable = reservation(STANDARD, 2, "2027-03-10", "2027-03-12", 2, RatePlan.NON_REFUNDABLE)
                .confirm().cancel();
        assertEquals(new BigDecimal("0.00"), nonRefundable.refund());
        assertEquals(nonRefundable.total(), nonRefundable.retained());

        Reservation inHouse = confirmed.doCheckIn();
        assertThrows(DomainException.class, inHouse::cancel);
        assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, assertThrows(DomainException.class, inHouse::confirm).code());
        assertThrows(DomainException.class, inHouse.doCheckOut()::doCheckOut);
    }

    @Test
    void capacityAndGuests() {
        assertEquals(ErrorCode.CAPACITY_EXCEEDED, assertThrows(DomainException.class,
                () -> reservation(STANDARD, 1, "2027-03-10", "2027-03-12", 3, RatePlan.STANDARD)).code());
        assertEquals(ErrorCode.INVALID_REQUEST, assertThrows(DomainException.class,
                () -> reservation(STANDARD, 1, "2027-03-10", "2027-03-12", 0, RatePlan.STANDARD)).code());
    }

    @Test
    void notifiersOnConfirmationAndCancellation() {
        Reservation confirmed = reservation(DELUXE, 7, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD).confirm();
        List<Message> sent = Notifiers.standard().stream()
                .flatMap(n -> n.messagesFor(new ReservationEvent.Confirmed(confirmed, DELUXE)).stream()).toList();
        assertEquals(3, sent.size());
        assertEquals("Reserva confirmada - Pousada Maré Alta", sent.get(0).subject());
        assertEquals("carla@example.com", sent.get(0).to());
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
                Até breve!""", sent.get(0).body());
        assertEquals("governanca@pousadamarealta.com.br", sent.get(1).to());
        assertEquals("Reserva confirmada - quarto 201", sent.get(1).subject());
        assertTrue(sent.get(1).body().contains("10/03/2027") && sent.get(1).body().contains("12/03/2027"));
        assertEquals("financeiro@pousadamarealta.com.br", sent.get(2).to());
        assertEquals("Pagamento recebido - reserva 7", sent.get(2).subject());
        assertTrue(sent.get(2).body().contains("R$ 779,80"));

        var cancelled = new ReservationEvent.Cancelled(confirmed.cancel(), DELUXE, true);
        List<Message> onCancel = Notifiers.standard().stream().flatMap(n -> n.messagesFor(cancelled).stream()).toList();
        assertEquals(List.of("Reserva cancelada - quarto 201", "Reembolso - reserva 7"),
                onCancel.stream().map(Message::subject).toList());
        assertTrue(onCancel.get(1).body().contains("R$ 779,80"));

        var nonRefundable = reservation(DELUXE, 9, "2027-03-10", "2027-03-12", 2, RatePlan.NON_REFUNDABLE).confirm();
        var noRefund = new ReservationEvent.Cancelled(nonRefundable.cancel(), DELUXE, true);
        assertEquals(List.of("Reserva cancelada - quarto 201"), Notifiers.standard().stream()
                .flatMap(n -> n.messagesFor(noRefund).stream()).map(Message::subject).toList());

        var pending = reservation(DELUXE, 10, "2027-03-10", "2027-03-12", 2, RatePlan.STANDARD);
        var silent = new ReservationEvent.Cancelled(pending.cancel(), DELUXE, false);
        assertTrue(Notifiers.standard().stream().flatMap(n -> n.messagesFor(silent).stream()).toList().isEmpty());
    }
}
