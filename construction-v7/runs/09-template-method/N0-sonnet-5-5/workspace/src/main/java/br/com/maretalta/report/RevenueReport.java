package br.com.maretalta.report;

import br.com.maretalta.ReservationService;
import br.com.maretalta.domain.Money;
import br.com.maretalta.domain.Reservation;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class RevenueReport implements Report {
    private final ReservationService service;

    public RevenueReport(ReservationService service) {
        this.service = service;
    }


    @Override public String name() { return "revenue"; }

    @Override public String paramName() { return "month"; }

    @Override
    public String generate(String rawParam) {
        YearMonth month = Csv.parseMonth(rawParam);
        return service.read(all -> {
            List<Reservation> rs = Csv.revenueReservations(all, month);
            List<String> rows = rs.stream()
                    .map(r -> String.join(",", String.valueOf(r.id), String.valueOf(r.room.number()),
                            r.checkIn.toString(), r.checkOut.toString(), r.status.name(),
                            Money.plain(r.netAmount())))
                    .toList();
            BigDecimal sum = rs.stream().map(Reservation::netAmount).reduce(Money.ZERO, BigDecimal::add);
            return Csv.build(name(), month.toString(), "reservation,room,check_in,check_out,status,amount",
                    rows, "total," + Money.plain(sum));
        });
    }
}
