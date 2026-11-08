package com.pousada.report;

import com.pousada.domain.DomainException;
import com.pousada.domain.ErrorCode;
import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

/** Base dos relatórios de um mês, sobre as reservas que começam nele. */
public abstract class MonthlyReport extends Report<YearMonth> {
    private final ReservationRepository reservations;

    protected MonthlyReport(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override
    public String parameter() {
        return "month";
    }

    @Override
    protected YearMonth parsePeriod(String raw) {
        try {
            return YearMonth.parse(raw);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "Mês inválido, use o formato 2027-07");
        }
    }

    @Override
    protected String periodLabel(YearMonth month) {
        return month.toString();
    }

    /** Reservas que começam no mês, por data de entrada e depois por número. */
    protected List<Reservation> startingIn(YearMonth month) {
        return reservations.findAll().stream()
                .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
                .sorted(Comparator.comparing(Reservation::getCheckIn).thenComparing(Reservation::getId))
                .toList();
    }

    /** As que deixaram dinheiro com a pousada: o que o financeiro e o relatório de planos contam. */
    protected List<Reservation> retainingMoney(YearMonth month) {
        return startingIn(month).stream().filter(r -> r.retainedAmount().signum() > 0).toList();
    }
}
