package br.com.marealta.pousada.service;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Reservation;
import br.com.marealta.pousada.persistence.ReservationRepository;
import br.com.marealta.pousada.persistence.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public class ReportService {
    private final RoomRepository rooms;
    private final ReservationRepository reservations;

    public ReportService(RoomRepository rooms, ReservationRepository reservations) {
        this.rooms = rooms;
        this.reservations = reservations;
    }

    public String occupancy(LocalDate night) {
        List<Reservation> occupying = reservations.findPaidOccupying(night);
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.room().number()).append(',').append(r.room().type()).append(',').append(r.id()).append(',')
                    .append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long occupiedRooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        return csv.append("total,").append(occupiedRooms).append('/').append(rooms.findAll().size()).append('\n')
                .toString();
    }

    public String revenue(YearMonth month) {
        List<Reservation> starting = reservations.findKeptMoneyStartingBetween(
                month.atDay(1), month.plusMonths(1).atDay(1));
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        BigDecimal total = Money.ZERO;
        for (Reservation r : starting) {
            csv.append(r.id()).append(',').append(r.room().number()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',').append(r.amountKept()).append('\n');
            total = total.add(r.amountKept());
        }
        return csv.append("total,").append(total).append('\n').toString();
    }
}
