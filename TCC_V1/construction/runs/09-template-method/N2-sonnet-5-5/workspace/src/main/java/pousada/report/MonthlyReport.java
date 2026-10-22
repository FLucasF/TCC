package pousada.report;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import pousada.db.ReservationRepository;
import pousada.domain.AppException;
import pousada.domain.ErrorCode;
import pousada.domain.Reservation;

/** Base dos relatórios de um mês: reservas que começam no mês, em ordem de entrada e número. */
abstract class MonthlyReport extends Report<YearMonth> {
    private final ReservationRepository reservations;

    MonthlyReport(ReservationRepository reservations) {
        this.reservations = reservations;
    }

    @Override
    public String parameter() {
        return "month";
    }

    @Override
    protected YearMonth parse(String raw) {
        try {
            if (raw == null || !raw.matches("\\d{4}-\\d{2}")) {
                throw new DateTimeParseException("mês", String.valueOf(raw), 0);
            }
            return YearMonth.parse(raw);
        } catch (DateTimeParseException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mês inválido, use o formato 2027-07");
        }
    }

    protected List<Reservation> startingIn(YearMonth month, Predicate<Reservation> filter) {
        return reservations.findAll().stream()
                .filter(r -> YearMonth.from(r.getCheckIn()).equals(month))
                .filter(filter)
                .sorted(Comparator.comparing(Reservation::getCheckIn).thenComparing(Reservation::getId))
                .toList();
    }

    /** Reservas que deixaram dinheiro com a pousada. */
    protected static boolean keptMoney(Reservation r) {
        return r.retained().signum() > 0;
    }
}
