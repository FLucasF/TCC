package br.com.mare.report;

import br.com.mare.domain.BusinessException;
import br.com.mare.domain.ErrorCode;
import br.com.mare.domain.Hotel;
import br.com.mare.domain.Reservation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

public class OccupancyReport extends Report<LocalDate> {
    private final ReservationQueries queries;

    public OccupancyReport(ReservationQueries queries) {
        this.queries = queries;
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
    protected LocalDate parse(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "date deve estar no formato 2027-07-10");
        }
    }

    @Override
    protected String periodLabel(LocalDate night) {
        return night.toString();
    }

    @Override
    protected String header() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected Body body(LocalDate night) {
        List<Reservation> occupying = queries.occupying(night);
        List<String> rows = occupying.stream()
                .map(r -> r.roomNumber() + "," + Hotel.requireRoom(r.roomNumber()).type() + "," + r.id()
                        + "," + r.checkIn() + "," + r.checkOut())
                .toList();
        long occupiedRooms = occupying.stream().map(Reservation::roomNumber).distinct().count();
        return new Body(rows, "total," + occupiedRooms + "/" + Hotel.rooms().size());
    }
}
