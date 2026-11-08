package com.marealta.report;

import com.marealta.Reservation;
import com.marealta.ReservationRepository;
import com.marealta.RoomCatalog;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** O que os relatórios consultam. */
public record ReportData(ReservationRepository reservations, RoomCatalog rooms) {

    public List<Reservation> startingIn(YearMonth month) {
        return reservations.startingBetween(month.atDay(1), month.plusMonths(1).atDay(1));
    }

    /** Reservas do mês que deixaram dinheiro com a pousada. */
    public List<Reservation> retainedIn(YearMonth month) {
        return startingIn(month).stream().filter(r -> r.retained().signum() > 0).toList();
    }

    /** Reservas pagas do mês (confirmadas, com check-in ou com check-out). */
    public List<Reservation> paidIn(YearMonth month) {
        return startingIn(month).stream().filter(r -> r.status().isPaid()).toList();
    }

    public List<Reservation> paidOccupying(LocalDate night) {
        return reservations.occupying(night).stream().filter(r -> r.status().isPaid()).toList();
    }
}
