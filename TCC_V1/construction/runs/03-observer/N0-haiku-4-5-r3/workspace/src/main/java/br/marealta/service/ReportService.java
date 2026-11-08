package br.marealta.service;

import br.marealta.db.ReservationDAO;
import br.marealta.db.RoomDAO;
import br.marealta.model.Reservation;
import br.marealta.model.Room;
import br.marealta.util.Formatters;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportService {
    private final ReservationDAO reservationDAO;
    private final RoomDAO roomDAO;

    public ReportService(ReservationDAO reservationDAO, RoomDAO roomDAO) {
        this.reservationDAO = reservationDAO;
        this.roomDAO = roomDAO;
    }

    public String generateOccupancyReport(LocalDate date) {
        List<Reservation> reservations = reservationDAO.findByDate(date);
        List<Room> allRooms = roomDAO.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("room,type,reservation,check_in,check_out\n");

        for (Room room : allRooms) {
            Reservation res = reservations.stream()
                .filter(r -> r.getRoomNumber() == room.getNumber())
                .findFirst()
                .orElse(null);

            if (res != null) {
                csv.append(String.format("%d,%s,%d,%s,%s\n",
                    room.getNumber(),
                    room.getType().name(),
                    res.getId(),
                    Formatters.formatDateForCsv(res.getCheckIn()),
                    Formatters.formatDateForCsv(res.getCheckOut())));
            }
        }

        int occupiedRooms = (int) reservations.stream().map(Reservation::getRoomNumber).distinct().count();
        csv.append(String.format("total,%d/%d\n", occupiedRooms, allRooms.size()));

        return csv.toString();
    }

    public String generateRevenueReport(int year, int month) {
        List<Reservation> reservations = reservationDAO.findByMonth(year, month);

        StringBuilder csv = new StringBuilder();
        csv.append("reservation,room,check_in,check_out,status,amount\n");

        BigDecimal total = BigDecimal.ZERO;

        for (Reservation res : reservations) {
            BigDecimal amount = res.getAmountPaid();

            if (res.getRefund() != null) {
                amount = amount.subtract(res.getRefund());
            }

            csv.append(String.format("%d,%d,%s,%s,%s,%s\n",
                res.getId(),
                res.getRoomNumber(),
                Formatters.formatDateForCsv(res.getCheckIn()),
                Formatters.formatDateForCsv(res.getCheckOut()),
                res.getStatus().name(),
                Formatters.formatMoneyForCsv(amount)));

            total = total.add(amount);
        }

        csv.append(String.format("total,%s", Formatters.formatMoneyForCsv(total)));

        return csv.toString();
    }
}
