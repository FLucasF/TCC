package br.com.mealta.report;

import br.com.mealta.domain.Money;
import br.com.mealta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Reservas do mês que deixaram dinheiro com a pousada. */
public class RevenueReport implements Report {
    private final YearMonth month;
    private final List<Reservation> kept;

    public RevenueReport(YearMonth month, List<Reservation> startingInMonth) {
        this.month = month;
        this.kept = keptMoney(startingInMonth);
    }

    /** As reservas que contam como dinheiro que ficou, para outros relatórios usarem a mesma regra. */
    public static List<Reservation> keptMoney(List<Reservation> startingInMonth) {
        return startingInMonth.stream().filter(Reservation::retainsMoney).toList();
    }

    @Override
    public String name() {
        return "revenue";
    }

    @Override
    public String period() {
        return month.toString();
    }

    @Override
    public List<String> columns() {
        return List.of("reservation", "room", "check_in", "check_out", "status", "amount");
    }

    @Override
    public List<List<String>> rows() {
        return kept.stream()
                .map(r -> List.of(String.valueOf(r.id()), String.valueOf(r.roomNumber()),
                        r.checkInDate().toString(), r.checkOutDate().toString(), r.status().name(),
                        Money.plain(r.keptAmount())))
                .toList();
    }

    @Override
    public List<String> total() {
        BigDecimal sum = kept.stream().map(Reservation::keptAmount).reduce(Money.ZERO, BigDecimal::add);
        return List.of("total", Money.plain(sum));
    }
}
