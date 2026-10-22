package br.com.pousadamareal.service;

import br.com.pousadamareal.domain.Reservation;
import br.com.pousadamareal.domain.Room;
import br.com.pousadamareal.repository.ReservationRepository;
import br.com.pousadamareal.repository.RoomRepository;
import br.com.pousadamareal.util.MoneyUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReportService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    public String getOccupancyReport(LocalDate date) throws SQLException {
        List<Room> allRooms = roomRepository.findAll();
        StringBuilder csv = new StringBuilder();

        csv.append("room,type,reservation,check_in,check_out\n");

        int occupiedCount = 0;
        for (Room room : allRooms) {
            List<Reservation> reservations = reservationRepository.findByRoomAndNight(room.getNumber(), date);
            if (!reservations.isEmpty()) {
                for (Reservation res : reservations) {
                    csv.append(room.getNumber()).append(",");
                    csv.append(room.getType().displayName()).append(",");
                    csv.append(res.getId()).append(",");
                    csv.append(res.getCheckIn()).append(",");
                    csv.append(res.getCheckOut()).append("\n");
                }
                occupiedCount++;
            }
        }

        csv.append("total,").append(occupiedCount).append("/").append(allRooms.size()).append("\n");

        return csv.toString();
    }

    public String getRevenueReport(int year, int month) throws SQLException {
        List<Reservation> reservations = reservationRepository.findByMonthAndPaid(year, month);
        StringBuilder csv = new StringBuilder();

        csv.append("reservation,room,check_in,check_out,status,amount\n");

        double totalAmount = 0;
        for (Reservation res : reservations) {
            csv.append(res.getId()).append(",");
            csv.append(res.getRoomNumber()).append(",");
            csv.append(res.getCheckIn()).append(",");
            csv.append(res.getCheckOut()).append(",");
            csv.append(res.getStatus().getValue()).append(",");

            double amount = res.getAmountPaid();
            if (res.getRefund() != null) {
                amount -= res.getRefund();
            }
            csv.append(MoneyUtils.format(amount)).append("\n");
            totalAmount += amount;
        }

        csv.append("total,").append(MoneyUtils.format(totalAmount)).append("\n");

        return csv.toString();
    }
}
