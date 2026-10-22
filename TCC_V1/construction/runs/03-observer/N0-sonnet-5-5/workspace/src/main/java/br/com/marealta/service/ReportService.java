package br.com.marealta.service;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.store.Store;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReportService {
    private final Store store;

    public ReportService(Store store) {
        this.store = store;
    }

    public String occupancy(LocalDate night) {
        return store.tx(tx -> {
            List<Room> rooms = tx.rooms();
            Map<Integer, Room> byNumber = rooms.stream().collect(Collectors.toMap(Room::number, Function.identity()));
            List<Reservation> rows = tx.all().stream()
                    .filter(r -> r.status().isPaid() && !r.checkIn().isAfter(night) && r.checkOut().isAfter(night))
                    .sorted(Comparator.comparingInt(Reservation::roomNumber).thenComparingLong(Reservation::id))
                    .toList();
            StringBuilder sb = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : rows) {
                sb.append(r.roomNumber()).append(',').append(byNumber.get(r.roomNumber()).type()).append(',')
                        .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
            }
            long occupied = rows.stream().map(Reservation::roomNumber).distinct().count();
            return sb.append("total,").append(occupied).append('/').append(rooms.size()).append('\n').toString();
        });
    }

    public String revenue(YearMonth month) {
        return store.tx(tx -> {
            List<Reservation> rows = tx.all().stream()
                    .filter(r -> YearMonth.from(r.checkIn()).equals(month) && r.retained().signum() > 0)
                    .sorted(Comparator.comparing(Reservation::checkIn).thenComparingLong(Reservation::id))
                    .toList();
            StringBuilder sb = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal total = Money.ZERO;
            for (Reservation r : rows) {
                sb.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                        .append(r.checkOut()).append(',').append(r.status()).append(',')
                        .append(r.retained().toPlainString()).append('\n');
                total = total.add(r.retained());
            }
            return sb.append("total,").append(Money.round(total).toPlainString()).append('\n').toString();
        });
    }
}
