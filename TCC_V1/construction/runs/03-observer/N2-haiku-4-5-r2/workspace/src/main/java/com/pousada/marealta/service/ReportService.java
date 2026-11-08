package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.ReservationStatus;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
  private static final int TOTAL_ROOMS = 5;

  public static String generateOccupancyReport(LocalDate date) throws SQLException {
    List<Reservation> reservations = getReservationsForDate(date);

    StringBuilder csv = new StringBuilder();
    csv.append("room,type,reservation,check_in,check_out\n");

    var rooms = RoomRepository.findAll();
    for (var room : rooms) {
      var reservation = reservations.stream()
          .filter(r -> r.getRoomNumber() == room.getNumber())
          .findFirst()
          .orElse(null);

      if (reservation != null && reservation.getStatus() != ReservationStatus.CANCELLED) {
        csv.append(reservation.getRoomNumber()).append(",");
        csv.append(room.getType()).append(",");
        csv.append(reservation.getId()).append(",");
        csv.append(formatDateForCsv(reservation.getCheckIn())).append(",");
        csv.append(formatDateForCsv(reservation.getCheckOut())).append("\n");
      }
    }

    long occupiedRooms = reservations.stream()
        .filter(r -> r.getStatus() != ReservationStatus.CANCELLED)
        .count();

    csv.append("total,").append(occupiedRooms).append("/").append(TOTAL_ROOMS);

    return csv.toString();
  }

  public static String generateRevenueReport(String month) throws SQLException {
    YearMonth yearMonth = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
    List<Reservation> reservations = ReservationRepository.findByMonthStarting(yearMonth.getYear(), yearMonth.getMonthValue());

    StringBuilder csv = new StringBuilder();
    csv.append("reservation,room,check_in,check_out,status,amount\n");

    BigDecimal total = BigDecimal.ZERO;

    for (Reservation reservation : reservations) {
      BigDecimal amount = calculateAmount(reservation);

      csv.append(reservation.getId()).append(",");
      csv.append(reservation.getRoomNumber()).append(",");
      csv.append(formatDateForCsv(reservation.getCheckIn())).append(",");
      csv.append(formatDateForCsv(reservation.getCheckOut())).append(",");
      csv.append(reservation.getStatus().name()).append(",");
      csv.append(formatNumberForCsv(amount)).append("\n");

      total = total.add(amount);
    }

    csv.append("total,,,,").append(formatNumberForCsv(total));

    return csv.toString();
  }

  private static BigDecimal calculateAmount(Reservation reservation) {
    BigDecimal amount = reservation.getAmountPaid();

    if (reservation.getRefund() != null && reservation.getRefund().compareTo(BigDecimal.ZERO) > 0) {
      amount = amount.subtract(reservation.getRefund());
    }

    return amount;
  }

  private static List<Reservation> getReservationsForDate(LocalDate date) throws SQLException {
    var rooms = RoomRepository.findAll();
    var occupiedReservations = new java.util.ArrayList<Reservation>();

    for (var room : rooms) {
      var reservations = ReservationRepository.findByRoomAndDate(room.getNumber(), date);
      occupiedReservations.addAll(reservations);
    }

    return occupiedReservations;
  }

  private static String formatDateForCsv(LocalDate date) {
    return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
  }

  private static String formatNumberForCsv(BigDecimal number) {
    return number.setScale(2, java.math.RoundingMode.HALF_EVEN).toPlainString();
  }
}
