package com.pousada.report;

import com.pousada.domain.DomainException;
import com.pousada.domain.ErrorCode;
import com.pousada.domain.Reservation;
import com.pousada.domain.RoomCatalog;
import com.pousada.repository.ReservationRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

public class OccupancyReport extends Report<LocalDate> {
    private final ReservationRepository reservations;
    private final RoomCatalog rooms;

    public OccupancyReport(ReservationRepository reservations, RoomCatalog rooms) {
        this.reservations = reservations;
        this.rooms = rooms;
    }

    @Override
    public String name() {
        return "occupancy";
    }

    @Override
    public String parameter() {
        return "date";
    }

    @Override
    protected LocalDate parsePeriod(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new DomainException(ErrorCode.INVALID_REQUEST, "Data inválida, use o formato 2027-07-10");
        }
    }

    @Override
    protected String periodLabel(LocalDate night) {
        return night.toString();
    }

    @Override
    protected List<String> header() {
        return List.of("room", "type", "reservation", "check_in", "check_out");
    }

    @Override
    protected Body body(LocalDate night) {
        List<Reservation> occupying = reservations.findAll().stream()
                .filter(r -> r.occupies(night))
                .sorted(Comparator.comparingInt(Reservation::getRoomNumber).thenComparing(Reservation::getId))
                .toList();
        List<List<String>> rows = occupying.stream().map(r -> List.of(
                String.valueOf(r.getRoomNumber()), rooms.get(r.getRoomNumber()).type().name(),
                String.valueOf(r.getId()), r.getCheckIn().toString(), r.getCheckOut().toString())).toList();
        long occupiedRooms = occupying.stream().map(Reservation::getRoomNumber).distinct().count();
        return new Body(rows, List.of("total", occupiedRooms + "/" + rooms.size()));
    }
}
