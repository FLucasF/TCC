package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import br.com.marealta.notification.FinanceNotifier;
import br.com.marealta.notification.GuestNotifier;
import br.com.marealta.notification.HousekeepingNotifier;
import br.com.marealta.notification.Message;
import br.com.marealta.notification.ReservationListener;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationTest {

    private final List<Message> sent = new ArrayList<>();
    private final List<ReservationListener> listeners = List.of(
            new GuestNotifier(sent::add),
            new HousekeepingNotifier(sent::add, "governanca@pousadamarealta.com.br"),
            new FinanceNotifier(sent::add, "financeiro@pousadamarealta.com.br"));

    private Reservation reservation(RatePlan plan) {
        Reservation r = Reservation.create(new Room(201, RoomType.DELUXE), "Carla Dias", "carla@example.com",
                "111.222.333-44", LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), 2, plan);
        r.assignId(7);
        return r;
    }

    private List<String> subjects() {
        return sent.stream().map(Message::subject).sorted().toList();
    }

    @Test
    void confirmationNotifiesGuestHousekeepingAndFinance() {
        Reservation r = reservation(RatePlan.STANDARD);
        r.confirm();
        listeners.forEach(l -> l.onConfirmed(r));

        assertEquals(List.of("Pagamento recebido - reserva 7", "Reserva confirmada - Pousada Maré Alta",
                "Reserva confirmada - quarto 201"), subjects());
        Message guest = sent.stream().filter(m -> m.to().equals("carla@example.com")).findFirst().orElseThrow();
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
    }

    @Test
    void cancellingPaidStandardNotifiesHousekeepingAndFinance() {
        Reservation r = reservation(RatePlan.STANDARD);
        r.confirm();
        r.cancel();
        listeners.forEach(l -> l.onCancelled(r));
        assertEquals(List.of("Reembolso - reserva 7", "Reserva cancelada - quarto 201"), subjects());
    }

    @Test
    void cancellingPaidNonRefundableDoesNotNotifyFinance() {
        Reservation r = reservation(RatePlan.NON_REFUNDABLE);
        r.confirm();
        r.cancel();
        listeners.forEach(l -> l.onCancelled(r));
        assertEquals(List.of("Reserva cancelada - quarto 201"), subjects());
    }

    @Test
    void cancellingPendingNotifiesNobody() {
        Reservation r = reservation(RatePlan.STANDARD);
        r.cancel();
        listeners.forEach(l -> l.onCancelled(r));
        assertEquals(List.of(), subjects());
    }
}
