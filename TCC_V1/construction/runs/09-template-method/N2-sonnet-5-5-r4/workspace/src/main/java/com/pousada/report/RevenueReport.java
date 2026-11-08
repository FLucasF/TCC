package com.pousada.report;

import com.pousada.domain.Money;
import com.pousada.domain.Reservation;
import com.pousada.repository.ReservationRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class RevenueReport extends MonthlyReport {
    public RevenueReport(ReservationRepository reservations) {
        super(reservations);
    }

    @Override
    public String name() {
        return "revenue";
    }

    @Override
    protected List<String> header() {
        return List.of("reservation", "room", "check_in", "check_out", "status", "amount");
    }

    @Override
    protected Body body(YearMonth month) {
        List<Reservation> kept = retainingMoney(month);
        List<List<String>> rows = kept.stream().map(r -> List.of(
                String.valueOf(r.getId()), String.valueOf(r.getRoomNumber()), r.getCheckIn().toString(),
                r.getCheckOut().toString(), r.getStatus().name(), Money.plain(r.retainedAmount()))).toList();
        BigDecimal total = kept.stream().map(Reservation::retainedAmount).reduce(Money.ZERO, BigDecimal::add);
        return new Body(rows, List.of("total", Money.plain(total)));
    }
}
