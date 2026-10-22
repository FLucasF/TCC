package pousada.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import pousada.domain.Money;
import pousada.domain.Reservation;
import pousada.domain.Room;
import pousada.persistence.ReservationRepository;
import pousada.persistence.RoomRepository;

public final class ReportService {
    private final RoomRepository rooms;
    private final ReservationRepository reservations;

    public ReportService(RoomRepository rooms, ReservationRepository reservations) {
        this.rooms = rooms;
        this.reservations = reservations;
    }

    public String occupancy(LocalDate night) {
        List<Room> allRooms = rooms.all();
        Map<Integer, Room> byNumber = allRooms.stream().collect(Collectors.toMap(Room::number, Function.identity()));
        List<Reservation> occupying = reservations.occupying(night);
        StringBuilder csv = new StringBuilder("room,type,reservation,check_in,check_out\n");
        for (Reservation r : occupying) {
            csv.append(r.roomNumber()).append(',').append(byNumber.get(r.roomNumber()).type()).append(',')
                    .append(r.id()).append(',').append(r.checkIn()).append(',').append(r.checkOut()).append('\n');
        }
        long occupiedRooms = occupying.stream().map(Reservation::roomNumber).distinct().count();
        return csv.append("total,").append(occupiedRooms).append('/').append(allRooms.size()).append('\n').toString();
    }

    public String revenue(YearMonth month) {
        StringBuilder csv = new StringBuilder("reservation,room,check_in,check_out,status,amount\n");
        var total = Money.ZERO;
        for (Reservation r : reservations.keptMoneyStartingIn(month)) {
            csv.append(r.id()).append(',').append(r.roomNumber()).append(',').append(r.checkIn()).append(',')
                    .append(r.checkOut()).append(',').append(r.status()).append(',').append(r.kept().toPlainString())
                    .append('\n');
            total = total.add(r.kept());
        }
        return csv.append("total,").append(total.toPlainString()).append('\n').toString();
    }
}
