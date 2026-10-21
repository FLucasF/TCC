package pousada;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public class RevenueReport extends CsvReport {
    private final List<Reservation> reservations;

    public RevenueReport(List<Reservation> reservations) {
        this.reservations = reservations.stream()
                .sorted(Comparator.comparing(Reservation::checkInDate).thenComparingInt(Reservation::id))
                .toList();
    }

    protected List<String> columns() {
        return List.of("reservation", "room", "check_in", "check_out", "status", "amount");
    }

    protected List<List<String>> rows() {
        return reservations.stream().map(r -> List.of(String.valueOf(r.id()),
                String.valueOf(r.room().number()), r.checkInDate().toString(), r.checkOutDate().toString(),
                r.getStatus().name(), Money.plain(r.retained()))).toList();
    }

    protected String total() {
        return Money.plain(reservations.stream().map(Reservation::retained)
                .reduce(Money.ZERO, BigDecimal::add));
    }
}
