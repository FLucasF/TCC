package br.com.pousadamarealta.reservas.service;

import br.com.pousadamarealta.reservas.domain.Money;
import br.com.pousadamarealta.reservas.domain.Reservation;
import br.com.pousadamarealta.reservas.domain.Room;
import br.com.pousadamarealta.reservas.persistence.Database;
import br.com.pousadamarealta.reservas.persistence.ReservationRepository;
import br.com.pousadamarealta.reservas.persistence.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Relatórios em CSV (valores como 599.76, datas como 2027-07-10, linhas terminadas em \n). */
public class ReportService {
    private final Database database;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;

    public ReportService(Database database, RoomRepository rooms, ReservationRepository reservations) {
        this.database = database;
        this.rooms = rooms;
        this.reservations = reservations;
    }

    public String occupancy(LocalDate night) {
        return database.inTransaction(c -> {
            Map<Integer, Room> byNumber = rooms.findAll(c).stream()
                    .collect(Collectors.toMap(Room::number, Function.identity()));
            List<Reservation> occupying = reservations.findPaidOnNight(c, night);
            StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
            for (Reservation r : occupying) {
                csv.append(r.roomNumber()).append(',')
                        .append(byNumber.get(r.roomNumber()).type()).append(',')
                        .append(r.id()).append(',')
                        .append(r.checkIn()).append(',')
                        .append(r.checkOut()).append('\n');
            }
            long occupied = occupying.stream().map(Reservation::roomNumber).distinct().count();
            csv.append("total,").append(occupied).append('/').append(byNumber.size()).append('\n');
            return csv.toString();
        });
    }

    public String revenue(YearMonth month) {
        return database.inTransaction(c -> {
            List<Reservation> list = reservations.findRetainingMoney(c, month.atDay(1), month.plusMonths(1).atDay(1));
            StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
            BigDecimal total = Money.ZERO;
            for (Reservation r : list) {
                BigDecimal amount = Money.scale(r.retained());
                total = total.add(amount);
                csv.append(r.id()).append(',')
                        .append(r.roomNumber()).append(',')
                        .append(r.checkIn()).append(',')
                        .append(r.checkOut()).append(',')
                        .append(r.status()).append(',')
                        .append(amount.toPlainString()).append('\n');
            }
            csv.append("total,").append(total.toPlainString()).append('\n');
            return csv.toString();
        });
    }
}
