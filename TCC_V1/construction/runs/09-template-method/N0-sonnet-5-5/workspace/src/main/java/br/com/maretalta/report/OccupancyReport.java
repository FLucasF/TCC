package br.com.maretalta.report;

import br.com.maretalta.domain.Reservation;
import br.com.maretalta.domain.Room;
import br.com.maretalta.service.ApiException;
import br.com.maretalta.store.Store;

import java.time.LocalDate;
import java.util.List;

public class OccupancyReport implements ReportType {
    @Override public String name() { return "occupancy"; }
    @Override public String periodParam() { return "date"; }

    @Override
    public Report build(Store.Tx tx, String rawPeriod) {
        if (rawPeriod == null || !rawPeriod.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw ApiException.invalid("Informe a data no formato 2027-07-10");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(rawPeriod);
        } catch (RuntimeException e) {
            throw ApiException.invalid("Data inválida: " + rawPeriod);
        }
        List<Reservation> occupying = tx.paidOccupying(date);
        List<List<String>> rows = occupying.stream().map(r -> List.of(
                String.valueOf(r.roomNumber()), r.room().type().name(), String.valueOf(r.id()),
                r.checkIn().toString(), r.checkOut().toString())).toList();
        long occupiedRooms = occupying.stream().map(Reservation::roomNumber).distinct().count();
        return new Report(name(), date.toString(),
                List.of("room", "type", "reservation", "check_in", "check_out"), rows,
                List.of("total", occupiedRooms + "/" + Room.ALL.size()));
    }
}
