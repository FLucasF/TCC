package com.pousada.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pousada.database.RoomRepository;
import com.pousada.model.Reservation;
import com.pousada.model.Room;
import com.pousada.service.ReservationService;
import com.pousada.service.FormattingService;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

public class ReservationsEndpoint {
  private static final Gson gson = new Gson();

  public static void createReservation(Context ctx) {
    try {
      JsonObject body = gson.fromJson(ctx.body(), JsonObject.class);

      if (body == null) {
        ctx.status(400).json(errorResponse("INVALID_REQUEST", "Invalid JSON"));
        return;
      }

      if (!body.has("roomNumber")
          || !body.has("guestName")
          || !body.has("guestEmail")
          || !body.has("guestCpf")
          || !body.has("checkIn")
          || !body.has("checkOut")
          || !body.has("guests")
          || !body.has("ratePlan")) {
        ctx.status(400).json(errorResponse("INVALID_REQUEST", "Missing required fields"));
        return;
      }

      ReservationService.CreateReservationRequest request =
          new ReservationService.CreateReservationRequest();
      request.roomNumber = body.get("roomNumber").getAsInt();
      request.guestName = body.get("guestName").getAsString();
      request.guestEmail = body.get("guestEmail").getAsString();
      request.guestCpf = body.get("guestCpf").getAsString();
      request.guests = body.get("guests").getAsInt();
      request.ratePlan = body.get("ratePlan").getAsString();

      try {
        request.checkIn = LocalDate.parse(body.get("checkIn").getAsString());
        request.checkOut = LocalDate.parse(body.get("checkOut").getAsString());
      } catch (DateTimeParseException e) {
        ctx.status(400).json(errorResponse("INVALID_REQUEST", "Invalid date format"));
        return;
      }

      Reservation reservation = ReservationService.createReservation(request);
      ctx.status(201).json(reservationToJson(reservation));
    } catch (ReservationService.ApiException e) {
      ctx.status(e.statusCode).json(errorResponse(e.code, e.getMessage()));
    } catch (Exception e) {
      ctx.status(400).json(errorResponse("INVALID_REQUEST", e.getMessage()));
    }
  }

  public static void getReservation(Context ctx) {
    try {
      long id = Long.parseLong(ctx.pathParam("id"));
      Optional<Reservation> reservation = com.pousada.database.ReservationRepository.findById(id);

      if (reservation.isEmpty()) {
        ctx.status(404).json(errorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        return;
      }

      ctx.json(reservationToJson(reservation.get()));
    } catch (NumberFormatException e) {
      ctx.status(404).json(errorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
    }
  }

  public static void confirmReservation(Context ctx) {
    try {
      long id = Long.parseLong(ctx.pathParam("id"));
      Reservation reservation = ReservationService.confirmReservation(id);
      ctx.json(reservationToJson(reservation));
    } catch (ReservationService.ApiException e) {
      ctx.status(e.statusCode).json(errorResponse(e.code, e.getMessage()));
    } catch (NumberFormatException e) {
      ctx.status(404).json(errorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
    }
  }

  public static void cancelReservation(Context ctx) {
    try {
      long id = Long.parseLong(ctx.pathParam("id"));
      Reservation reservation = ReservationService.cancelReservation(id);
      ctx.json(reservationToJson(reservation));
    } catch (ReservationService.ApiException e) {
      ctx.status(e.statusCode).json(errorResponse(e.code, e.getMessage()));
    } catch (NumberFormatException e) {
      ctx.status(404).json(errorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
    }
  }

  public static void checkIn(Context ctx) {
    try {
      long id = Long.parseLong(ctx.pathParam("id"));
      Reservation reservation = ReservationService.checkIn(id);
      ctx.json(reservationToJson(reservation));
    } catch (ReservationService.ApiException e) {
      ctx.status(e.statusCode).json(errorResponse(e.code, e.getMessage()));
    } catch (NumberFormatException e) {
      ctx.status(404).json(errorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
    }
  }

  public static void checkOut(Context ctx) {
    try {
      long id = Long.parseLong(ctx.pathParam("id"));
      Reservation reservation = ReservationService.checkOut(id);

      Optional<Room> room = RoomRepository.findByNumber(reservation.roomNumber);
      String roomType = room.isPresent() ? room.get().type : "UNKNOWN";

      JsonObject receipt = new JsonObject();
      receipt.addProperty("title", "Comprovante de reserva");
      receipt.addProperty("reservationId", reservation.id);
      receipt.addProperty("guestName", reservation.guestName);
      receipt.addProperty("guestCpf", reservation.guestCpf);
      receipt.addProperty("roomNumber", reservation.roomNumber);
      receipt.addProperty("roomType", roomType);
      receipt.addProperty("checkIn", FormattingService.formatDate(reservation.checkIn));
      receipt.addProperty("checkOut", FormattingService.formatDate(reservation.checkOut));
      receipt.addProperty("nights", reservation.nights);
      receipt.addProperty("subtotal", reservation.subtotal);
      if (reservation.discount.compareTo(java.math.BigDecimal.ZERO) > 0) {
        receipt.addProperty("discount", reservation.discount);
      }
      receipt.addProperty("total", reservation.total);

      ctx.json(receipt);
    } catch (ReservationService.ApiException e) {
      ctx.status(e.statusCode).json(errorResponse(e.code, e.getMessage()));
    } catch (NumberFormatException e) {
      ctx.status(404).json(errorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
    }
  }

  public static void getDocument(Context ctx) {
    try {
      long id = Long.parseLong(ctx.pathParam("id"));
      Optional<Reservation> reservation =
          com.pousada.database.ReservationRepository.findById(id);

      if (reservation.isEmpty()) {
        ctx.status(404).json(errorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
        return;
      }

      Reservation res = reservation.get();
      if ("PENDING".equals(res.status) || "CANCELLED".equals(res.status)) {
        ctx.status(409)
            .json(errorResponse("OPERATION_NOT_ALLOWED", "Document only available for paid reservations"));
        return;
      }

      Optional<Room> room = RoomRepository.findByNumber(res.roomNumber);
      String roomType = room.isPresent() ? room.get().type : "UNKNOWN";

      JsonObject receipt = new JsonObject();
      receipt.addProperty("title", "Comprovante de reserva");
      receipt.addProperty("reservationId", res.id);
      receipt.addProperty("guestName", res.guestName);
      receipt.addProperty("guestCpf", res.guestCpf);
      receipt.addProperty("roomNumber", res.roomNumber);
      receipt.addProperty("roomType", roomType);
      receipt.addProperty("checkIn", FormattingService.formatDate(res.checkIn));
      receipt.addProperty("checkOut", FormattingService.formatDate(res.checkOut));
      receipt.addProperty("nights", res.nights);
      receipt.addProperty("subtotal", res.subtotal);
      if (res.discount.compareTo(java.math.BigDecimal.ZERO) > 0) {
        receipt.addProperty("discount", res.discount);
      }
      receipt.addProperty("total", res.total);

      ctx.json(receipt);
    } catch (NumberFormatException e) {
      ctx.status(404).json(errorResponse("RESERVATION_NOT_FOUND", "Reservation not found"));
    }
  }

  private static JsonObject reservationToJson(Reservation reservation) {
    JsonObject obj = new JsonObject();
    obj.addProperty("id", reservation.id);
    obj.addProperty("roomNumber", reservation.roomNumber);
    obj.addProperty("guestName", reservation.guestName);
    obj.addProperty("guestEmail", reservation.guestEmail);
    obj.addProperty("guestCpf", reservation.guestCpf);
    obj.addProperty("checkIn", FormattingService.formatCsvDate(reservation.checkIn));
    obj.addProperty("checkOut", FormattingService.formatCsvDate(reservation.checkOut));
    obj.addProperty("guests", reservation.guests);
    obj.addProperty("ratePlan", reservation.ratePlan);
    obj.addProperty("nights", reservation.nights);
    obj.addProperty("subtotal", reservation.subtotal);
    obj.addProperty("discount", reservation.discount);
    obj.addProperty("total", reservation.total);
    obj.addProperty("status", reservation.status);
    obj.addProperty("amountPaid", reservation.amountPaid);
    if (reservation.refund != null) {
      obj.addProperty("refund", reservation.refund);
    } else {
      obj.add("refund", null);
    }
    return obj;
  }

  private static JsonObject errorResponse(String code, String message) {
    JsonObject error = new JsonObject();
    error.addProperty("error", code);
    error.addProperty("message", message);
    return error;
  }
}
