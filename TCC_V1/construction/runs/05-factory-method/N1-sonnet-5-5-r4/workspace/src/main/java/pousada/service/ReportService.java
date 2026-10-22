package pousada.service;

import pousada.domain.Money;
import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.domain.RoomCatalog;
import pousada.persistence.Database;
import pousada.persistence.ReservationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class ReportService {
    private final Database db;
    private final ReservationRepository reservations = new ReservationRepository();

    public ReportService(Database db) {
        this.db = db;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = db.tx(c -> reservations.paidOccupying(c, night));
        List<String> lines = new ArrayList<>();
        lines.add("room,type,reservation,check_in,check_out");
        for (Reservation r : occupying) {
            Room room = RoomCatalog.require(r.roomNumber());
            lines.add(String.join(",", String.valueOf(room.number()), room.type().name(), String.valueOf(r.id()),
                    r.checkIn().toString(), r.checkOut().toString()));
        }
        lines.add("total," + occupying.size() + "/" + RoomCatalog.all().size());
        return toCsv(lines);
    }

    public String revenue(YearMonth month) {
        List<Reservation> kept = db.tx(c ->
                reservations.keptMoneyStartingBetween(c, month.atDay(1), month.plusMonths(1).atDay(1)));
        List<String> lines = new ArrayList<>();
        lines.add("reservation,room,check_in,check_out,status,amount");
        BigDecimal total = Money.ZERO;
        for (Reservation r : kept) {
            BigDecimal amount = Money.round(r.amountPaid().subtract(r.refund() == null ? Money.ZERO : r.refund()));
            total = total.add(amount);
            lines.add(String.join(",", String.valueOf(r.id()), String.valueOf(r.roomNumber()),
                    r.checkIn().toString(), r.checkOut().toString(), r.status().name(), amount.toPlainString()));
        }
        lines.add("total," + total.toPlainString());
        return toCsv(lines);
    }

    private static String toCsv(List<String> lines) {
        return String.join("\n", lines) + "\n";
    }
}
