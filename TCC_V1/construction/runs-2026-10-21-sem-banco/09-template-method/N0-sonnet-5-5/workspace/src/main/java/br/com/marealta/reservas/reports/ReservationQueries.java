package br.com.marealta.reservas.reports;

import br.com.marealta.reservas.domain.Reservation;
import br.com.marealta.reservas.domain.ReservationService;

import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/** Seleções de reservas compartilhadas entre relatórios, para que usem sempre o mesmo critério. */
final class ReservationQueries {
    static final Comparator<Reservation> BY_CHECK_IN_THEN_ID =
            Comparator.comparing(Reservation::checkIn).thenComparingInt(Reservation::id);

    private ReservationQueries() {
    }

    /** Reservas pagas (confirmadas, com check-in ou check-out) que começam no mês. */
    static List<Reservation> paidStartingIn(ReservationService service, YearMonth month) {
        return startingIn(service, month).stream().filter(r -> r.status().isPaid()).toList();
    }

    /** Reservas que começam no mês e deixaram dinheiro com a pousada. */
    static List<Reservation> keptMoneyStartingIn(ReservationService service, YearMonth month) {
        return startingIn(service, month).stream().filter(r -> r.amountKept().signum() > 0).toList();
    }

    private static List<Reservation> startingIn(ReservationService service, YearMonth month) {
        return service.all().stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month))
                .sorted(BY_CHECK_IN_THEN_ID)
                .toList();
    }
}
