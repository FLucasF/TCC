package br.com.marealta.app;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.persistence.ReservationRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public final class ReportService {

    private final ReservationRepository repository;
    private final RoomCatalog rooms;

    public ReportService(ReservationRepository repository, RoomCatalog rooms) {
        this.repository = repository;
        this.rooms = rooms;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = repository.findPaidOccupying(night);
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.room().number()).append(',')
                    .append(r.room().type()).append(',')
                    .append(r.id()).append(',')
                    .append(r.checkInDate()).append(',')
                    .append(r.checkOutDate()).append('\n');
        }
        long occupiedRooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        csv.append("total,").append(occupiedRooms).append('/').append(rooms.all().size()).append('\n');
        return csv.toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> starting = repository.findCheckingInBetween(month.atDay(1), month.plusMonths(1).atDay(1));
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : starting) {
            if (r.retained().signum() <= 0) {
                continue;
            }
            csv.append(r.id()).append(',')
                    .append(r.room().number()).append(',')
                    .append(r.checkInDate()).append(',')
                    .append(r.checkOutDate()).append(',')
                    .append(r.status()).append(',')
                    .append(r.retained().toPlainString()).append('\n');
            total = total.add(r.retained());
        }
        csv.append("total,").append(total.toPlainString()).append('\n');
        return csv.toString();
    }
}
