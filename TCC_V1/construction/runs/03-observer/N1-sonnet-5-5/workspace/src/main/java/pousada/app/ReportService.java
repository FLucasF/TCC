package pousada.app;

import pousada.domain.*;
import pousada.infra.Database;
import pousada.infra.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {
    private final Database db;
    private final ReservationRepository repository;
    private final RoomCatalog rooms;

    public ReportService(Database db, ReservationRepository repository, RoomCatalog rooms) {
        this.db = db;
        this.repository = repository;
        this.rooms = rooms;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = db.inTransaction(c -> repository.findPaidOccupying(c, night));
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            row(csv, r.room().number(), r.room().type(), r.id(), r.checkIn(), r.checkOut());
        }
        return row(csv, "total", occupying.size() + "/" + rooms.size()).toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> starting = db.inTransaction(c ->
                repository.findCheckingInBetween(c, month.atDay(1), month.plusMonths(1).atDay(1)));
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : starting) {
            if (r.retained().signum() > 0) {
                row(csv, r.id(), r.room().number(), r.checkIn(), r.checkOut(), r.status(), r.retained());
                total = total.add(r.retained());
            }
        }
        return row(csv, "total", total).toString();
    }

    private static StringBuilder row(StringBuilder csv, Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            csv.append(i == 0 ? "" : ",").append(cells[i] instanceof BigDecimal b ? b.toPlainString() : cells[i]);
        }
        return csv.append('\n');
    }
}
