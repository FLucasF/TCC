package br.com.marealta.pousada.service;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Room;
import br.com.marealta.pousada.domain.RoomCatalog;
import br.com.marealta.pousada.persistence.Database;
import br.com.marealta.pousada.persistence.ReservationRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {

    private final Database database;
    private final ReservationRepository reservations;

    public ReportService(Database database, ReservationRepository reservations) {
        this.database = database;
        this.reservations = reservations;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = query(c -> reservations.findPaidOccupying(c, night));
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            Room room = RoomCatalog.find(r.roomNumber()).orElseThrow();
            csv.append(room.number()).append(',').append(room.type()).append(',').append(r.id()).append(',')
                    .append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long occupiedRooms = occupying.stream().map(Reservation::roomNumber).distinct().count();
        csv.append("total,").append(occupiedRooms).append('/').append(RoomCatalog.all().size()).append('\n');
        return csv.toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> starting = query(c ->
                reservations.findStartingBetween(c, month.atDay(1), month.plusMonths(1).atDay(1)));
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : starting) {
            BigDecimal retained = r.retained();
            if (retained.signum() <= 0) {
                continue;
            }
            csv.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',')
                    .append(retained.toPlainString()).append('\n');
            total = total.add(retained);
        }
        csv.append("total,").append(total.toPlainString()).append('\n');
        return csv.toString();
    }

    private <T> T query(Database.Work<T> work) {
        try {
            return database.inTransaction(work);
        } catch (SQLException e) {
            throw new IllegalStateException("Erro no banco de dados", e);
        }
    }
}
