package com.pousada.service;

import com.pousada.model.Reservation;
import com.pousada.model.Room;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ReportService {
  private final ReservationRepository reservationRepository;
  private final RoomRepository roomRepository;

  public ReportService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
    this.reservationRepository = reservationRepository;
    this.roomRepository = roomRepository;
  }

  public String occupancyReport(LocalDate date) {
    StringBuilder csv = new StringBuilder();
    csv.append("room,type,reservation,check_in,check_out\n");

    List<Room> rooms = roomRepository.findAll();
    int occupiedCount = 0;

    for (Room room : rooms) {
      List<Reservation> reservations = reservationRepository.findConfirmedByRoomAndDate(room.getNumber(), date);
      if (!reservations.isEmpty()) {
        for (Reservation res : reservations) {
          csv.append(room.getNumber()).append(",")
              .append(room.getType()).append(",")
              .append(res.getId()).append(",")
              .append(formatDate(res.getCheckIn())).append(",")
              .append(formatDate(res.getCheckOut())).append("\n");
        }
        occupiedCount++;
      }
    }

    csv.append("total,").append(occupiedCount).append("/").append(rooms.size()).append("\n");

    return csv.toString();
  }

  public String revenueReport(int year, int month) {
    StringBuilder csv = new StringBuilder();
    csv.append("reservation,room,check_in,check_out,status,amount\n");

    List<Reservation> reservations = reservationRepository.findByMonthWithRevenue(year, month);
    BigDecimal totalRevenue = BigDecimal.ZERO;

    for (Reservation res : reservations) {
      BigDecimal revenue = res.getAmountPaid();
      if (res.getRefund() != null) {
        revenue = revenue.subtract(res.getRefund());
      }

      csv.append(res.getId()).append(",")
          .append(res.getRoomNumber()).append(",")
          .append(formatDate(res.getCheckIn())).append(",")
          .append(formatDate(res.getCheckOut())).append(",")
          .append(res.getStatus()).append(",")
          .append(formatMoney(revenue)).append("\n");

      totalRevenue = totalRevenue.add(revenue);
    }

    csv.append("total,,,,").append(formatMoney(totalRevenue)).append("\n");

    return csv.toString();
  }

  private String formatDate(LocalDate date) {
    return String.format("%04d-%02d-%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
  }

  private String formatMoney(BigDecimal amount) {
    return String.format("%.2f", amount);
  }
}
