package com.marealta.events;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.marealta.domain.Channel;
import com.marealta.domain.MessageOutbox;
import com.marealta.domain.RatePlan;
import com.marealta.domain.Reservation;
import com.marealta.domain.ReservationStatus;
import com.marealta.domain.Room;
import com.marealta.domain.RoomCatalog;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListenersTest {
    record Sent(String to, String subject, String body) { }

    private final List<Sent> sent = new ArrayList<>();
    private final MessageOutbox outbox = (Channel c, String to, String subject, String body) ->
            sent.add(new Sent(to, subject, body));
    private final List<ReservationListener> listeners = List.of(
            new GuestConfirmationEmail(outbox),
            new HousekeepingNotifier(outbox, "gov@x"),
            new FinanceNotifier(outbox, "fin@x"));
    private final Room room = new RoomCatalog().require(201);

    private Reservation reservation(RatePlan plan) {
        Reservation r = Reservation.open(room, "Carla Dias", "carla@x.com", "111.222.333-44",
                LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, plan);
        r.assignId(7);
        return r;
    }

    private void publish(ReservationEvent.Type type, Reservation r, ReservationStatus previous) {
        listeners.forEach(l -> l.onEvent(new ReservationEvent(type, r, room, previous)));
    }

    @Test
    void confirmationNotifiesGuestHousekeepingAndFinance() {
        Reservation r = reservation(RatePlan.STANDARD);
        r.confirm();
        publish(ReservationEvent.Type.CONFIRMED, r, ReservationStatus.PENDING);

        assertEquals(3, sent.size());
        Sent guest = sent.stream().filter(s -> s.to().equals("carla@x.com")).findFirst().orElseThrow();
        assertEquals("Reserva confirmada - Pousada Maré Alta", guest.subject());
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
                Até breve!""", guest.body());
        Sent gov = sent.stream().filter(s -> s.to().equals("gov@x")).findFirst().orElseThrow();
        assertEquals("Reserva confirmada - quarto 201", gov.subject());
        assertEquals(true, gov.body().contains("10/03/2027") && gov.body().contains("12/03/2027"));
        Sent fin = sent.stream().filter(s -> s.to().equals("fin@x")).findFirst().orElseThrow();
        assertEquals("Pagamento recebido - reserva 7", fin.subject());
        assertEquals(true, fin.body().contains("R$ 779,80"));
    }

    @Test
    void cancelledConfirmedReservationNotifiesHousekeepingAndFinanceWhenThereIsRefund() {
        Reservation r = reservation(RatePlan.STANDARD);
        r.confirm();
        r.cancel();
        publish(ReservationEvent.Type.CANCELLED, r, ReservationStatus.CONFIRMED);
        assertEquals(List.of("Reserva cancelada - quarto 201", "Reembolso - reserva 7"),
                sent.stream().map(Sent::subject).sorted().toList().reversed());
    }

    @Test
    void nonRefundableCancellationDoesNotNotifyFinance() {
        Reservation r = reservation(RatePlan.NON_REFUNDABLE);
        r.confirm();
        r.cancel();
        publish(ReservationEvent.Type.CANCELLED, r, ReservationStatus.CONFIRMED);
        assertEquals(List.of("Reserva cancelada - quarto 201"), sent.stream().map(Sent::subject).toList());
    }

    @Test
    void cancelledPendingReservationNotifiesNobody() {
        Reservation r = reservation(RatePlan.STANDARD);
        r.cancel();
        publish(ReservationEvent.Type.CANCELLED, r, ReservationStatus.PENDING);
        assertEquals(0, sent.size());
    }
}
