package com.pousadamarealta.services;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.infrastructure.ReservationRepository;
import com.pousadamarealta.infrastructure.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public static String getOccupancyReport(LocalDate date) throws Exception {
    List<Reservation> reservations = ReservationRepository.findConfirmedOnDate(date);
    var rooms = RoomRepository.findAll();

    StringBuilder csv = new StringBuilder();
    csv.append("room,type,reservation,check_in,check_out\n");

    var occupiedRoomNumbers = new java.util.HashSet<Integer>();
    for (var room : rooms) {
      var roomReservation = reservations.stream()
          .filter(r -> r.roomNumber == room.number)
          .findFirst();

      if (roomReservation.isPresent()) {
        var res = roomReservation.get();
        csv.append(room.number).append(",")
            .append(room.type.value).append(",")
            .append(res.id).append(",")
            .append(res.checkIn.format(DATE_FORMATTER)).append(",")
            .append(res.checkOut.format(DATE_FORMATTER)).append("\n");
        occupiedRoomNumbers.add(room.number);
      }
    }

    csv.append("total,").append(occupiedRoomNumbers.size()).append("/").append(rooms.size());

    return csv.toString();
  }

  public static String getRevenueReport(String monthStr) throws Exception {
    String[] parts = monthStr.split("-");
    if (parts.length != 2) {
      throw new Exception("INVALID_REQUEST");
    }

    int year = Integer.parseInt(parts[0]);
    int month = Integer.parseInt(parts[1]);

    List<Reservation> reservations = ReservationRepository.findByMonthStartingInMonth(month, year);

    StringBuilder csv = new StringBuilder();
    csv.append("reservation,room,check_in,check_out,status,amount\n");

    BigDecimal total = BigDecimal.ZERO;
    for (var res : reservations) {
      if (res.amountPaid.signum() > 0) {
        BigDecimal amount = res.amountPaid.subtract(res.refund != null ? res.refund : BigDecimal.ZERO);
        csv.append(res.id).append(",")
            .append(res.roomNumber).append(",")
            .append(res.checkIn.format(DATE_FORMATTER)).append(",")
            .append(res.checkOut.format(DATE_FORMATTER)).append(",")
            .append(res.status.value).append(",")
            .append(String.format("%.2f", amount)).append("\n");
        total = total.add(amount);
      }
    }

    csv.append("total,,,,,").append(String.format("%.2f", total));

    return csv.toString();
  }
}
