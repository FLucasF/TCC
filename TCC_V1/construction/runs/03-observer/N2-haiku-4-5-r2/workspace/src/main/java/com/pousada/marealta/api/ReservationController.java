package com.pousada.marealta.api;

import com.pousada.marealta.domain.Reservation;
import com.pousada.marealta.domain.RatePlan;
import com.pousada.marealta.domain.Receipt;
import com.pousada.marealta.repository.RoomRepository;
import com.pousada.marealta.repository.ReservationRepository;
import com.pousada.marealta.service.PricingCalculator;
import com.pousada.marealta.service.ReceiptFormatter;
import com.pousada.marealta.service.ReservationService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReservationController {
  private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public static void createReservation(Context ctx) {
    try {
      ReservationRequest request = ctx.bodyAsClass(ReservationRequest.class);

      if (request.roomNumber == 0 || request.guestName == null || request.guestEmail == null ||
          request.guestCpf == null || request.checkIn == null || request.checkOut == null ||
          request.ratePlan == null) {
        ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Missing required fields"));
        return;
      }

      LocalDate checkIn = parseDate(request.checkIn);
      LocalDate checkOut = parseDate(request.checkOut);

      if (checkIn == null || checkOut == null) {
        ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        return;
      }

      RatePlan ratePlan = RatePlan.valueOf(request.ratePlan.toUpperCase());

      try {
        Reservation reservation = ReservationService.createReservation(
            request.roomNumber, request.guestName, request.guestEmail, request.guestCpf,
            checkIn, checkOut, request.guests, ratePlan);

        ctx.status(201).json(ReservationConverter.toResponse(reservation));
      } catch (IllegalArgumentException e) {
        handleServiceException(ctx, e.getMessage());
      }
    } catch (IllegalArgumentException e) {
      ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid rate plan"));
    } catch (Exception e) {
      logger.error("Error creating reservation", e);
      ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Failed to create reservation"));
    }
  }

  public static void getReservation(Context ctx) {
    try {
      long id = ctx.pathParamAsClass("id", Long.class).get();
      Reservation reservation = ReservationRepository.findById(id);

      if (reservation == null) {
        ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        return;
      }

      ctx.json(ReservationConverter.toResponse(reservation));
    } catch (Exception e) {
      logger.error("Error fetching reservation", e);
      ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Failed to fetch reservation"));
    }
  }

  public static void confirmReservation(Context ctx) {
    try {
      long id = ctx.pathParamAsClass("id", Long.class).get();
      Reservation reservation = ReservationRepository.findById(id);

      if (reservation == null) {
        ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        return;
      }

      try {
        Reservation confirmed = ReservationService.confirmReservation(id, reservation.getTotal());
        ctx.json(ReservationConverter.toResponse(confirmed));
      } catch (IllegalArgumentException e) {
        handleServiceException(ctx, e.getMessage());
      }
    } catch (Exception e) {
      logger.error("Error confirming reservation", e);
      ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Failed to confirm reservation"));
    }
  }

  public static void checkIn(Context ctx) {
    try {
      long id = ctx.pathParamAsClass("id", Long.class).get();

      try {
        Reservation checkedIn = ReservationService.checkIn(id);
        ctx.json(ReservationConverter.toResponse(checkedIn));
      } catch (IllegalArgumentException e) {
        handleServiceException(ctx, e.getMessage());
      }
    } catch (Exception e) {
      logger.error("Error checking in reservation", e);
      ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Failed to check in reservation"));
    }
  }

  public static void checkOut(Context ctx) {
    try {
      long id = ctx.pathParamAsClass("id", Long.class).get();

      try {
        Reservation checkedOut = ReservationService.checkOut(id);
        ctx.json(ReservationConverter.toResponse(checkedOut));
      } catch (IllegalArgumentException e) {
        handleServiceException(ctx, e.getMessage());
      }
    } catch (Exception e) {
      logger.error("Error checking out reservation", e);
      ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Failed to check out reservation"));
    }
  }

  public static void cancelReservation(Context ctx) {
    try {
      long id = ctx.pathParamAsClass("id", Long.class).get();

      try {
        Reservation cancelled = ReservationService.cancelReservation(id);
        ctx.json(ReservationConverter.toResponse(cancelled));
      } catch (IllegalArgumentException e) {
        handleServiceException(ctx, e.getMessage());
      }
    } catch (Exception e) {
      logger.error("Error cancelling reservation", e);
      ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Failed to cancel reservation"));
    }
  }

  public static void getReceipt(Context ctx) {
    try {
      long id = ctx.pathParamAsClass("id", Long.class).get();
      Reservation reservation = ReservationRepository.findById(id);

      if (reservation == null) {
        ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        return;
      }

      if (reservation.getAmountPaid().compareTo(java.math.BigDecimal.ZERO) <= 0) {
        ctx.status(404).json(new ErrorResponse("RECEIPT_NOT_FOUND", "Receipt not found"));
        return;
      }

      var room = RoomRepository.findByNumber(reservation.getRoomNumber());

      var receipt = new ReceiptResponse(
          "Comprovante de reserva",
          reservation.getId(),
          reservation.getGuestName(),
          reservation.getGuestCpf(),
          reservation.getRoomNumber(),
          room.getType(),
          reservation.getCheckIn().format(DATE_FORMATTER),
          reservation.getCheckOut().format(DATE_FORMATTER),
          reservation.getNights(),
          reservation.getSubtotal().doubleValue(),
          reservation.getDiscount().doubleValue(),
          reservation.getTotal().doubleValue()
      );

      ctx.json(receipt);
    } catch (Exception e) {
      logger.error("Error fetching receipt", e);
      ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Failed to fetch receipt"));
    }
  }

  private static LocalDate parseDate(String dateStr) {
    try {
      return LocalDate.parse(dateStr, DATE_FORMATTER);
    } catch (Exception e) {
      return null;
    }
  }

  private static void handleServiceException(Context ctx, String error) {
    switch (error) {
      case "ROOM_NOT_FOUND" -> ctx.status(404).json(new ErrorResponse("ROOM_NOT_FOUND", "Room not found"));
      case "RESERVATION_NOT_FOUND" ->
          ctx.status(404).json(new ErrorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
      case "ROOM_UNAVAILABLE" ->
          ctx.status(409).json(new ErrorResponse("ROOM_UNAVAILABLE", "Room is not available for the selected dates"));
      case "CAPACITY_EXCEEDED" ->
          ctx.status(422).json(new ErrorResponse("CAPACITY_EXCEEDED", "Number of guests exceeds room capacity"));
      case "OPERATION_NOT_ALLOWED" ->
          ctx.status(409).json(new ErrorResponse("OPERATION_NOT_ALLOWED", "This operation is not allowed for the current reservation status"));
      case "INVALID_REQUEST" ->
          ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid request"));
      default ->
          ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "An error occurred"));
    }
  }
}
