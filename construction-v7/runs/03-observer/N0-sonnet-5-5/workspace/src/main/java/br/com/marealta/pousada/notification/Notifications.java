package br.com.marealta.pousada.notification;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Receipt;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.event.EventBus;
import br.com.marealta.pousada.event.ReservationEvent;

/** Wires every notification audience to the reservation events. */
public class Notifications {
    public static final String GOVERNANCE = "governanca@pousadamarealta.com.br";
    public static final String FINANCE = "financeiro@pousadamarealta.com.br";

    private final MessageLog log;

    private Notifications(MessageLog log) {
        this.log = log;
    }

    public static void register(EventBus bus, MessageLog log) {
        Notifications n = new Notifications(log);
        bus.subscribe(ReservationEvent.Confirmed.class, n::guestConfirmed);
        bus.subscribe(ReservationEvent.Confirmed.class, n::governanceConfirmed);
        bus.subscribe(ReservationEvent.Confirmed.class, n::financeConfirmed);
        bus.subscribe(ReservationEvent.Cancelled.class, n::governanceCancelled);
        bus.subscribe(ReservationEvent.Cancelled.class, n::financeCancelled);
    }

    private void email(String to, String subject, String body) {
        log.send(Channel.EMAIL, to, subject, body);
    }

    private void guestConfirmed(ReservationEvent.Confirmed e) {
        Reservation r = e.reservation();
        String body = "Olá, " + r.guestName() + "!\n\n"
                + "Sua reserva na Pousada Maré Alta está confirmada. Estes são os dados dela:\n\n"
                + Receipt.of(r).text() + "\n\n"
                + "A entrada é a partir das 14h, e a saída, até as 12h.\n"
                + "Até breve!";
        email(r.guestEmail(), "Reserva confirmada - Pousada Maré Alta", body);
    }

    private void governanceConfirmed(ReservationEvent.Confirmed e) {
        Reservation r = e.reservation();
        String body = "Reserva nº " + r.id() + " confirmada para o quarto " + r.room().number()
                + " (" + r.room().type() + ").\n"
                + "Entrada: " + Receipt.formatDate(r.checkIn()) + "\n"
                + "Saída: " + Receipt.formatDate(r.checkOut()) + "\n"
                + "Hóspedes: " + r.guests();
        email(GOVERNANCE, "Reserva confirmada - quarto " + r.room().number(), body);
    }

    private void financeConfirmed(ReservationEvent.Confirmed e) {
        Reservation r = e.reservation();
        String body = "Pagamento recebido da reserva nº " + r.id() + ": " + Money.format(r.amountPaid()) + ".";
        email(FINANCE, "Pagamento recebido - reserva " + r.id(), body);
    }

    private void governanceCancelled(ReservationEvent.Cancelled e) {
        if (!e.wasConfirmed()) {
            return;
        }
        Reservation r = e.reservation();
        String body = "A reserva nº " + r.id() + " do quarto " + r.room().number() + " foi cancelada.\n"
                + "Datas liberadas: de " + Receipt.formatDate(r.checkIn())
                + " (entrada) a " + Receipt.formatDate(r.checkOut()) + " (saída).";
        email(GOVERNANCE, "Reserva cancelada - quarto " + r.room().number(), body);
    }

    private void financeCancelled(ReservationEvent.Cancelled e) {
        Reservation r = e.reservation();
        if (!e.wasConfirmed() || r.refund() == null || r.refund().signum() <= 0) {
            return;
        }
        String body = "Reembolso da reserva nº " + r.id() + ": valor a devolver " + Money.format(r.refund()) + ".";
        email(FINANCE, "Reembolso - reserva " + r.id(), body);
    }
}
