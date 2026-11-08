package pousada.report;

import pousada.domain.Reservation;
import pousada.persistence.Database;

import java.time.LocalDate;
import java.util.List;

public class ReportService {
    private final Database database;

    public ReportService(Database database) {
        this.database = database;
    }

    public String generate(Report report, String periodText) {
        Period period = report.periodKind().read(periodText);
        ReportTable table = database.inTransaction(tx -> report.build(period, new ReportSource() {
            @Override
            public List<Reservation> occupying(LocalDate night) {
                return tx.reservations().occupying(night);
            }

            @Override
            public List<Reservation> startingIn(Period p) {
                return tx.reservations().startingIn(p.from(), p.toExclusive());
            }
        }));
        return CsvWriter.write(report, period, table);
    }
}
