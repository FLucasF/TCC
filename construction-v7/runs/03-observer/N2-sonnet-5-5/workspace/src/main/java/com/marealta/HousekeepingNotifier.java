package com.marealta;

/** Avisa a governança de confirmações e cancelamentos de reservas pagas. */
public class HousekeepingNotifier implements ReservationListener {
    static final String ADDRESS = "governanca@pousadamarealta.com.br";
    private final Outbox outbox;

    public HousekeepingNotifier(Outbox outbox) { this.outbox = outbox; }

    @Override
    public void onConfirmed(Reservation r) {
        outbox.send(Channel.EMAIL, ADDRESS, "Reserva confirmada - quarto " + r.room().number(),
                "O quarto " + r.room().number() + " foi reservado.\n"
                + "Entrada: " + Money.brDate(r.checkIn()) + "\n"
                + "Saída: " + Money.brDate(r.checkOut()) + "\n"
                + "Hóspedes: " + r.guests());
    }

    @Override
    public void onPaidReservationCancelled(Reservation r) {
        outbox.send(Channel.EMAIL, ADDRESS, "Reserva cancelada - quarto " + r.room().number(),
                "A reserva do quarto " + r.room().number() + " foi cancelada.\n"
                + "Datas liberadas: de " + Money.brDate(r.checkIn()) + " a " + Money.brDate(r.checkOut()));
    }
}
