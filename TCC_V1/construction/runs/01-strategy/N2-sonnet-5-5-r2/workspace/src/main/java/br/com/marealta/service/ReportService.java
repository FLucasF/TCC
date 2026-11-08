package br.com.marealta.service;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Rooms;
import br.com.marealta.persistence.Database;
import br.com.marealta.persistence.ReservationRepository;

import java.math.BigDecimal;
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
        List<Reservation> occupying = database.inTransaction(c -> reservations.findPaidOccupying(c, night));
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.room().number()).append(',').append(r.room().type()).append(',').append(r.id()).append(',')
                    .append(r.checkInDate()).append(',').append(r.checkOutDate()).append('\n');
        }
        long occupiedRooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        return csv.append("total,").append(occupiedRooms).append('/').append(Rooms.all().size()).append('\n')
                .toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> starting = database.inTransaction(
                c -> reservations.findCheckingInBetween(c, month.atDay(1), month.atEndOfMonth()));
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : starting) {
            if (r.retained().signum() <= 0) {
                continue;
            }
            csv.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkInDate()).append(',')
                    .append(r.checkOutDate()).append(',').append(r.status()).append(',')
                    .append(r.retained().toPlainString()).append('\n');
            total = total.add(r.retained());
        }
        return csv.append("total,").append(Money.round(total).toPlainString()).append('\n').toString();
    }
}
