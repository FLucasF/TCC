package pousada.report;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import pousada.domain.Reservation;
import pousada.error.BusinessException;
import pousada.error.ErrorCode;
import pousada.service.ReservationRepository;
import pousada.service.RoomCatalog;

public class OccupancyReport implements Report {
    private final ReservationRepository repository;
    private final RoomCatalog rooms;

    public OccupancyReport(ReservationRepository repository, RoomCatalog rooms) {
        this.repository = repository;
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
    public ReportData generate(String period) {
        LocalDate night = parse(period);
        List<Reservation> occupying = repository.all().stream()
                .filter(r -> r.status().isPaid() && r.occupiesNight(night))
                .sorted(Comparator.comparing((Reservation r) -> r.room().number()).thenComparing(Reservation::id))
                .toList();
        List<List<String>> rows = occupying.stream()
                .map(r -> List.of(String.valueOf(r.room().number()), r.room().type().name(),
                        String.valueOf(r.id()), r.checkIn().toString(), r.checkOut().toString()))
                .toList();
        long occupiedRooms = occupying.stream().map(r -> r.room().number()).distinct().count();
        return new ReportData(night.toString(),
                List.of("room", "type", "reservation", "check_in", "check_out"), rows,
                List.of("total", occupiedRooms + "/" + rooms.all().size()));
    }

    private static LocalDate parse(String period) {
        try {
            return LocalDate.parse(period);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Data inválida, use o formato 2027-07-10");
        }
    }
}
