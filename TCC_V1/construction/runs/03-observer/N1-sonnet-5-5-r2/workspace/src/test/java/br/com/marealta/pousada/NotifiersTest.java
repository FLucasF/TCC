package br.com.marealta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.marealta.pousada.domain.ConfirmedReservationCancelled;
import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.ReservationConfirmed;
import br.com.marealta.pousada.notification.Channel;
import br.com.marealta.pousada.notification.EventBus;
import br.com.marealta.pousada.notification.Notifiers;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class NotifiersTest {

    record Sent(String to, String subject, String body) {
    }

    final List<Sent> sent = new ArrayList<>();
    final EventBus bus = Notifiers.wire((Channel c, String to, String subject, String body) ->
            sent.add(new Sent(to, subject, body)));

    @Test
    void confirmationNotifiesGuestHousekeepingAndFinance() {
        Reservation r = DomainTest.open(RatePlan.STANDARD);
        r.confirm();
        bus.publish(new ReservationConfirmed(r));

        assertEquals(3, sent.size());
        Sent guest = find("carla@x.com");
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
        assertEquals("Reserva confirmada - quarto 201", find("governanca@pousadamarealta.com.br").subject());
        Sent finance = find("financeiro@pousadamarealta.com.br");
        assertEquals("Pagamento recebido - reserva 7", finance.subject());
        assertEquals(true, finance.body().contains("R$ 779,80"));
    }

    @Test
    void cancellationSkipsFinanceWhenNothingToRefund() {
        Reservation r = DomainTest.open(RatePlan.NON_REFUNDABLE);
        r.confirm();
        r.cancel();
        bus.publish(new ConfirmedReservationCancelled(r));
        assertEquals(1, sent.size());
        assertEquals("Reserva cancelada - quarto 201", sent.get(0).subject());
    }

    @Test
    void cancellationWithRefundNotifiesFinance() {
        Reservation r = DomainTest.open(RatePlan.STANDARD);
        r.confirm();
        r.cancel();
        bus.publish(new ConfirmedReservationCancelled(r));
        assertEquals(2, sent.size());
        assertEquals("Reembolso - reserva 7", find("financeiro@pousadamarealta.com.br").subject());
    }

    private Sent find(String to) {
        return sent.stream().filter(s -> s.to().equals(to)).findFirst().orElseThrow();
    }
}
