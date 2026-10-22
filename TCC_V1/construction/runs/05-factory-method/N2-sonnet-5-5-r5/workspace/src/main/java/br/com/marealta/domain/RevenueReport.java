package br.com.marealta.domain;

import java.math.BigDecimal;
import java.util.List;

public final class RevenueReport extends CsvReport {
    private final List<Reservation> kept;

    /** Recebe as reservas do mês já em ordem de entrada e número. */
    public RevenueReport(List<Reservation> startingInMonth) {
        this.kept = startingInMonth.stream().filter(r -> r.amountKept().signum() > 0).toList();
    }

    @Override
    protected String header() {
        return "reservation,room,check_in,check_out,status,amount";
    }

    @Override
    protected List<String> rows() {
        return kept.stream()
                .map(r -> r.id() + "," + r.room().number() + "," + r.checkIn() + "," + r.checkOut() + ","
                        + r.status() + "," + r.amountKept().toPlainString())
                .toList();
    }

    @Override
    protected String totalLine() {
        BigDecimal sum = kept.stream().map(Reservation::amountKept).reduce(Money.ZERO, BigDecimal::add);
        return "total," + Money.round(sum).toPlainString();
    }
}
