package pousada.report;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import pousada.domain.Reservation;
import pousada.error.BusinessException;
import pousada.error.ErrorCode;
import pousada.service.ReservationRepository;

/** Base dos relatórios que olham as reservas que começam em um mês. */
abstract class MonthlyReport implements Report {
    private final ReservationRepository repository;

    MonthlyReport(ReservationRepository repository) {
        this.repository = repository;
    }

    @Override
    public String parameter() {
        return "month";
    }

    @Override
    public ReportData generate(String period) {
        return generate(parse(period));
    }

    abstract ReportData generate(YearMonth month);

    /** Reservas que começam no mês, em ordem de entrada e depois de número. */
    List<Reservation> startingIn(YearMonth month) {
        return repository.all().stream()
                .filter(r -> YearMonth.from(r.checkIn()).equals(month))
                .sorted(Comparator.comparing(Reservation::checkIn).thenComparing(Reservation::id))
                .toList();
    }

    /** Reservas do mês que deixaram dinheiro com a pousada. */
    List<Reservation> keptMoneyStartingIn(YearMonth month) {
        return startingIn(month).stream().filter(r -> r.heldAmount().signum() > 0).toList();
    }

    private static YearMonth parse(String period) {
        try {
            return YearMonth.parse(period);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Mês inválido, use o formato 2027-07");
        }
    }
}
