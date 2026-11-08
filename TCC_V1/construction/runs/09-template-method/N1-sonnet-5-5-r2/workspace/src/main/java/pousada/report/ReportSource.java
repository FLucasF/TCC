package pousada.report;

import pousada.domain.Reservation;

import java.time.LocalDate;
import java.util.List;

/** De onde os relatórios tiram as reservas; os recortes comuns a mais de um relatório ficam aqui. */
public interface ReportSource {
    /** Reservas, de qualquer situação, que ocupam a noite, em ordem de quarto. */
    List<Reservation> occupying(LocalDate night);

    /** Reservas, de qualquer situação, que começam no período, em ordem de entrada e número. */
    List<Reservation> startingIn(Period period);

    default List<Reservation> paidOccupying(LocalDate night) {
        return occupying(night).stream().filter(r -> r.status().isPaid()).toList();
    }

    default List<Reservation> paidStartingIn(Period period) {
        return startingIn(period).stream().filter(r -> r.status().isPaid()).toList();
    }

    /** As que deixaram dinheiro com a pousada: o pago menos o devolvido é maior que zero. */
    default List<Reservation> retainedStartingIn(Period period) {
        return startingIn(period).stream().filter(r -> r.retained().signum() > 0).toList();
    }
}
