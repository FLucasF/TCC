package br.com.mare.report;

import br.com.mare.domain.Reservation;
import br.com.mare.persistence.JdbcReservationRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/** Seleções de reservas que os relatórios compartilham. */
public class ReservationQueries {
    private static final Comparator<Reservation> BY_CHECK_IN_THEN_ID =
            Comparator.comparing(Reservation::checkIn).thenComparing(Reservation::id);

    private final JdbcReservationRepository reservations;

    public ReservationQueries(JdbcReservationRepository reservations) {
        this.reservations = reservations;
    }

    /** Reservas pagas que ocupam um quarto na noite, por número do quarto. */
    public List<Reservation> occupying(LocalDate night) {
        return reservations.findAll().stream()
                .filter(Reservation::isPaid)
                .filter(r -> r.occupies(night))
                .sorted(Comparator.comparingInt(Reservation::roomNumber))
                .toList();
    }

    /** Reservas pagas que começam no mês. */
    public List<Reservation> paidStartingIn(YearMonth month) {
        return startingIn(month).filter(Reservation::isPaid).toList();
    }

    /** Reservas que começam no mês e deixaram dinheiro com a pousada. */
    public List<Reservation> retainedStartingIn(YearMonth month) {
        return startingIn(month).filter(r -> r.retained().signum() > 0).toList();
    }

    private java.util.stream.Stream<Reservation> startingIn(YearMonth month) {
        return reservations.findAll().stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month))
                .sorted(BY_CHECK_IN_THEN_ID);
    }
}
