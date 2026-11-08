package br.com.marealta.report;

import br.com.marealta.domain.Reservation;
import br.com.marealta.domain.Room;
import br.com.marealta.repository.ReservationRepository;
import br.com.marealta.repository.RoomRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OccupancyReport extends CsvReport {
    private final LocalDate date;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

    public OccupancyReport(LocalDate date, ReservationRepository reservationRepository,
                          RoomRepository roomRepository) {
        this.date = date;
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    protected String getReportName() {
        return "occupancy";
    }

    @Override
    protected String getPeriod() {
        return date.format(dateFormatter);
    }

    @Override
    protected String buildHeaderLine() {
        return "room,type,reservation,check_in,check_out";
    }

    @Override
    protected List<String> buildDataLines() {
        List<Reservation> reservations = reservationRepository.findPaidByDate(date);
        List<String> lines = new ArrayList<>();

        for (Reservation reservation : reservations) {
            Room room = roomRepository.findByNumber(reservation.getRoomNumber()).get();
            lines.add(String.join(",",
                    String.valueOf(room.getNumber()),
                    room.getType().name(),
                    String.valueOf(reservation.getId()),
                    reservation.getCheckIn().format(dateFormatter),
                    reservation.getCheckOut().format(dateFormatter)
            ));
        }

        return lines;
    }

    @Override
    protected String buildTotalLine() {
        List<Reservation> reservations = reservationRepository.findPaidByDate(date);
        int totalRooms = roomRepository.findAll().size();
        return "total," + reservations.size() + "/" + totalRooms;
    }
}
