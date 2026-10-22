package com.pousada.service;

import com.pousada.database.ReservationRepository;
import com.pousada.database.RoomRepository;
import com.pousada.event.EventDispatcher;
import com.pousada.event.ReservationCancelledEvent;
import com.pousada.event.ReservationConfirmedEvent;
import com.pousada.model.Reservation;
import com.pousada.model.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationService {
  public static class CreateReservationRequest {
    public int roomNumber;
    public String guestName;
    public String guestEmail;
    public String guestCpf;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public int guests;
    public String ratePlan;
  }

  public static Reservation createReservation(CreateReservationRequest request) {
    Optional<Room> room = RoomRepository.findByNumber(request.roomNumber);
    if (room.isEmpty()) {
      throw new ApiException("ROOM_NOT_FOUND", "Room not found", 404);
    }

    if (request.guests < 1) {
      throw new ApiException("INVALID_REQUEST", "At least 1 guest required", 400);
    }

    if (request.guests > room.get().capacity) {
      throw new ApiException("CAPACITY_EXCEEDED", "Too many guests for this room", 422);
    }

    if (request.checkOut.isBefore(request.checkIn) || request.checkOut.equals(request.checkIn)) {
      throw new ApiException("INVALID_REQUEST", "Check-out must be after check-in", 400);
    }

    List<Reservation> conflicting =
        ReservationRepository.findByRoomAndDateRange(
            request.roomNumber, request.checkIn, request.checkOut);
    if (!conflicting.isEmpty()) {
      throw new ApiException("ROOM_UNAVAILABLE", "Room not available for these dates", 409);
    }

    PricingService.PriceCalculation pricing =
        PricingService.calculate(request.checkIn, request.checkOut, room.get().nightlyRate, request.ratePlan);

    return ReservationRepository.create(
        request.roomNumber,
        request.guestName,
        request.guestEmail,
        request.guestCpf,
        request.checkIn,
        request.checkOut,
        request.guests,
        request.ratePlan,
        pricing.nights,
        pricing.subtotal,
        pricing.discount,
        pricing.total);
  }

  public static Reservation confirmReservation(long id) {
    Optional<Reservation> reservation = ReservationRepository.findById(id);
    if (reservation.isEmpty()) {
      throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
    }

    if (!"PENDING".equals(reservation.get().status)) {
      throw new ApiException("OPERATION_NOT_ALLOWED", "Can only confirm pending reservations", 409);
    }

    Reservation res = reservation.get();
    ReservationRepository.confirmReservation(id, res.total);

    Reservation updated = ReservationRepository.findById(id).get();
    EventDispatcher.dispatchConfirmed(new ReservationConfirmedEvent(updated, res.total));

    return updated;
  }

  public static Reservation cancelReservation(long id) {
    Optional<Reservation> reservation = ReservationRepository.findById(id);
    if (reservation.isEmpty()) {
      throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
    }

    String status = reservation.get().status;
    if ("CHECKED_IN".equals(status) || "CHECKED_OUT".equals(status) || "CANCELLED".equals(status)) {
      throw new ApiException(
          "OPERATION_NOT_ALLOWED", "Can only cancel pending or confirmed reservations", 409);
    }

    Reservation res = reservation.get();
    boolean wasConfirmed = "CONFIRMED".equals(status);
    BigDecimal refund = null;

    if (wasConfirmed) {
      if ("STANDARD".equals(res.ratePlan)) {
        refund = res.amountPaid;
      } else if ("NON_REFUNDABLE".equals(res.ratePlan)) {
        refund = BigDecimal.ZERO;
      }
    }

    ReservationRepository.cancelReservation(id, refund);

    Reservation updated = ReservationRepository.findById(id).get();
    EventDispatcher.dispatchCancelled(new ReservationCancelledEvent(updated, wasConfirmed, refund));

    return updated;
  }

  public static Reservation checkIn(long id) {
    Optional<Reservation> reservation = ReservationRepository.findById(id);
    if (reservation.isEmpty()) {
      throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
    }

    if (!"CONFIRMED".equals(reservation.get().status)) {
      throw new ApiException("OPERATION_NOT_ALLOWED", "Can only check in confirmed reservations", 409);
    }

    ReservationRepository.updateStatus(id, "CHECKED_IN");
    return ReservationRepository.findById(id).get();
  }

  public static Reservation checkOut(long id) {
    Optional<Reservation> reservation = ReservationRepository.findById(id);
    if (reservation.isEmpty()) {
      throw new ApiException("RESERVATION_NOT_FOUND", "Reservation not found", 404);
    }

    if (!"CHECKED_IN".equals(reservation.get().status)) {
      throw new ApiException("OPERATION_NOT_ALLOWED", "Can only check out if checked in", 409);
    }

    ReservationRepository.updateStatus(id, "CHECKED_OUT");
    return ReservationRepository.findById(id).get();
  }

  public static class ApiException extends RuntimeException {
    public final String code;
    public final int statusCode;

    public ApiException(String code, String message, int statusCode) {
      super(message);
      this.code = code;
      this.statusCode = statusCode;
    }
  }
}
