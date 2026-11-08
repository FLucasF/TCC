package com.pousadamarealta.services;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.ReservationStatus;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.infrastructure.PriceCalculator;
import com.pousadamarealta.infrastructure.ReservationRepository;
import com.pousadamarealta.infrastructure.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public class ReservationService {
  public static Reservation create(
      int roomNumber,
      String guestName,
      String guestEmail,
      String guestCpf,
      LocalDate checkIn,
      LocalDate checkOut,
      int guests,
      RatePlan ratePlan)
      throws Exception {

    Room room =
        RoomRepository.findById(roomNumber)
            .orElseThrow(() -> new Exception("ROOM_NOT_FOUND"));

    if (guests < 1 || guests > room.capacity) {
      throw new Exception(
          guests > room.capacity ? "CAPACITY_EXCEEDED" : "INVALID_REQUEST");
    }

    if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
      throw new Exception("INVALID_REQUEST");
    }

    var existingReservations = ReservationRepository.findByRoomAndDateRange(roomNumber, checkIn, checkOut);
    if (!existingReservations.isEmpty()) {
      throw new Exception("ROOM_UNAVAILABLE");
    }

    int nights = (int) java.time.temporal.ChronoUnit.DAYS.between(checkIn, checkOut);

    BigDecimal subtotal = BigDecimal.ZERO;
    for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
      BigDecimal nightPrice = PriceCalculator.calculateNightlyPrice(room.nightlyRate, date);
      subtotal = subtotal.add(nightPrice);
    }
    subtotal = PriceCalculator.round(subtotal);

    BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, ratePlan.discountRate);
    BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

    return ReservationRepository.insert(
        roomNumber,
        guestName,
        guestEmail,
        guestCpf,
        checkIn,
        checkOut,
        guests,
        ratePlan,
        nights,
        subtotal,
        discount,
        total);
  }

  public static Reservation confirm(long id, BigDecimal amountPaid) throws Exception {
    Reservation res = ReservationRepository.findById(id)
        .orElseThrow(() -> new Exception("RESERVATION_NOT_FOUND"));

    if (!res.status.equals(ReservationStatus.PENDING)) {
      throw new Exception("OPERATION_NOT_ALLOWED");
    }

    ReservationRepository.confirmPayment(id, amountPaid);
    Reservation confirmed = ReservationRepository.findById(id).orElseThrow();

    ReservationEventPublisher.publishConfirmed(new ReservationEvent(ReservationEvent.EventType.CONFIRMED, confirmed));

    return confirmed;
  }

  public static Reservation cancel(long id) throws Exception {
    Reservation res = ReservationRepository.findById(id)
        .orElseThrow(() -> new Exception("RESERVATION_NOT_FOUND"));

    if (res.status.equals(ReservationStatus.CHECKED_IN) || res.status.equals(ReservationStatus.CHECKED_OUT) || res.status.equals(ReservationStatus.CANCELLED)) {
      throw new Exception("OPERATION_NOT_ALLOWED");
    }

    BigDecimal refund = BigDecimal.ZERO;
    if (res.status.equals(ReservationStatus.CONFIRMED) && res.ratePlan.equals(RatePlan.STANDARD)) {
      refund = res.amountPaid;
    }

    ReservationRepository.cancelWithRefund(id, refund);
    Reservation cancelled = ReservationRepository.findById(id).orElseThrow();

    ReservationEventPublisher.publishCancelled(new ReservationEvent(ReservationEvent.EventType.CANCELLED, cancelled, res));

    return cancelled;
  }

  public static Reservation checkIn(long id) throws Exception {
    Reservation res = ReservationRepository.findById(id)
        .orElseThrow(() -> new Exception("RESERVATION_NOT_FOUND"));

    if (!res.status.equals(ReservationStatus.CONFIRMED)) {
      throw new Exception("OPERATION_NOT_ALLOWED");
    }

    ReservationRepository.updateStatus(id, ReservationStatus.CHECKED_IN);
    Reservation checkedIn = ReservationRepository.findById(id).orElseThrow();

    ReservationEventPublisher.publishCheckedIn(new ReservationEvent(ReservationEvent.EventType.CHECKED_IN, checkedIn));

    return checkedIn;
  }

  public static Reservation checkOut(long id) throws Exception {
    Reservation res = ReservationRepository.findById(id)
        .orElseThrow(() -> new Exception("RESERVATION_NOT_FOUND"));

    if (!res.status.equals(ReservationStatus.CHECKED_IN)) {
      throw new Exception("OPERATION_NOT_ALLOWED");
    }

    ReservationRepository.updateStatus(id, ReservationStatus.CHECKED_OUT);
    Reservation checkedOut = ReservationRepository.findById(id).orElseThrow();

    ReservationEventPublisher.publishCheckedOut(new ReservationEvent(ReservationEvent.EventType.CHECKED_OUT, checkedOut));

    return checkedOut;
  }
}
