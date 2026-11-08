package br.com.marealta.pousada.service;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.domain.Room;
import br.com.marealta.pousada.domain.RoomType;
import br.com.marealta.pousada.persistence.Database;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Relatórios em CSV (linhas separadas por \n, valores com ponto, datas ISO). */
public class ReportService {
    private final Database db;

    public ReportService(Database db) {
        this.db = db;
    }

    public String occupancy(LocalDate night) {
        return db.inTransaction(tx -> {
            Map<Integer, RoomType> types = tx.rooms().findAll().stream()
                    .collect(Collectors.toMap(Room::number, Room::type));
            List<Reservation> occupying = tx.reservations().findPaidOccupying(night);
            StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : occupying) {
                csv.append(r.roomNumber()).append(',').append(types.get(r.roomNumber())).append(',')
                        .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
            }
            long occupied = occupying.stream().map(Reservation::roomNumber).distinct().count();
            return csv.append("total,").append(occupied).append('/').append(types.size()).append('\n').toString();
        });
    }

    public String revenue(YearMonth month) {
        return db.inTransaction(tx -> {
            List<Reservation> list = tx.reservations()
                    .findKeepingMoneyStartingBetween(month.atDay(1), month.plusMonths(1).atDay(1));
            StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal total = Money.ZERO;
            for (Reservation r : list) {
                csv.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                        .append(r.checkOut()).append(',').append(r.status()).append(',')
                        .append(r.kept().toPlainString()).append('\n');
                total = total.add(r.kept());
            }
            return csv.append("total,").append(total.toPlainString()).append('\n').toString();
        });
    }
}
