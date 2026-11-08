package com.pousada.marealta.service;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.ReservationStatus;
import com.pousada.marealta.domain.RatePlan;
import com.pousada.marealta.domain.Room;
import com.pousada.marealta.notification.NotificationService;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.repository.RoomRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

public class ReservationService {

  public static Reservation createReservation(int roomNumber, String guestName, String guestEmail,
      String guestCpf, LocalDate checkIn, LocalDate checkOut, int guests, RatePlan ratePlan)
      throws SQLException {

    Room room = RoomRepository.findByNumber(roomNumber);
    if (room == null) {
      throw new IllegalArgumentException("ROOM_NOT_FOUND");
    }

    if (guests < 1) {
      throw new IllegalArgumentException("INVALID_REQUEST");
    }

    if (guests > room.getCapacity()) {
      throw new IllegalArgumentException("CAPACITY_EXCEEDED");
    }

    if (!checkIn.isBefore(checkOut)) {
      throw new IllegalArgumentException("INVALID_REQUEST");
    }

    checkRoomAvailability(roomNumber, checkIn, checkOut);

    int nights = PricingCalculator.calculateNights(checkIn, checkOut);
    BigDecimal subtotal = PricingCalculator.calculateSubtotal(room.getNightlyRate(), checkIn, checkOut);
    BigDecimal discount = PricingCalculator.calculateDiscount(subtotal, ratePlan.getDiscountRate());
    BigDecimal total = PricingCalculator.calculateTotal(subtotal, discount);

    Reservation reservation = new Reservation(0, roomNumber, guestName, guestEmail, guestCpf,
        checkIn, checkOut, guests, ratePlan, nights, subtotal, discount, total);

    return ReservationRepository.create(reservation);
  }

  public static Reservation confirmReservation(long id, BigDecimal amountPaid) throws SQLException {
    Reservation reservation = ReservationRepository.findById(id);
    if (reservation == null) {
      throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
    }

    if (reservation.getStatus() != ReservationStatus.PENDING) {
      throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
    }

    reservation.setStatus(ReservationStatus.CONFIRMED);
    reservation.setAmountPaid(amountPaid);
    ReservationRepository.update(reservation);

    NotificationService.notifyReservationConfirmed(reservation);

    return reservation;
  }

  public static Reservation checkIn(long id) throws SQLException {
    Reservation reservation = ReservationRepository.findById(id);
    if (reservation == null) {
      throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
    }

    if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
      throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
    }

    reservation.setStatus(ReservationStatus.CHECKED_IN);
    ReservationRepository.update(reservation);

    return reservation;
  }

  public static Reservation checkOut(long id) throws SQLException {
    Reservation reservation = ReservationRepository.findById(id);
    if (reservation == null) {
      throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
    }

    if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
      throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
    }

    reservation.setStatus(ReservationStatus.CHECKED_OUT);
    ReservationRepository.update(reservation);

    return reservation;
  }

  public static Reservation cancelReservation(long id) throws SQLException {
    Reservation reservation = ReservationRepository.findById(id);
    if (reservation == null) {
      throw new IllegalArgumentException("RESERVATION_NOT_FOUND");
    }

    ReservationStatus status = reservation.getStatus();
    if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
      throw new IllegalArgumentException("OPERATION_NOT_ALLOWED");
    }

    reservation.setStatus(ReservationStatus.CANCELLED);

    if (status == ReservationStatus.CONFIRMED) {
      if (reservation.getRatePlan() == RatePlan.STANDARD) {
        reservation.setRefund(reservation.getAmountPaid());
      } else {
        reservation.setRefund(BigDecimal.ZERO);
      }
    } else {
      reservation.setRefund(BigDecimal.ZERO);
    }

    ReservationRepository.update(reservation);

    if (status == ReservationStatus.CONFIRMED) {
      NotificationService.notifyReservationCancelled(reservation);
    }

    return reservation;
  }

  private static void checkRoomAvailability(int roomNumber, LocalDate checkIn, LocalDate checkOut)
      throws SQLException {
    LocalDate currentDate = checkIn;
    while (currentDate.isBefore(checkOut)) {
      var reservations = ReservationRepository.findByRoomAndDate(roomNumber, currentDate);
      if (!reservations.isEmpty()) {
        throw new IllegalArgumentException("ROOM_UNAVAILABLE");
      }
      currentDate = currentDate.plusDays(1);
    }
  }
}
