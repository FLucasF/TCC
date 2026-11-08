package com.pousada.api;

import io.javalin.Javalin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pousada.api.dto.ReservationRequest;
import com.pousada.model.Document;
import com.pousada.model.Reservation;
import com.pousada.rateplan.RatePlanStrategy;
import com.pousada.repository.MessageRepository;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.ReservationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ReservationController {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final ReservationRepository reservationRepository = new ReservationRepository();
  private static final RoomRepository roomRepository = new RoomRepository();
  private static final MessageRepository messageRepository = new MessageRepository();
  private static final ReservationService reservationService =
      new ReservationService(reservationRepository, roomRepository, messageRepository);

  public static void routes(Javalin app) {
    app.post("/reservations", ctx -> {
      try {
        ReservationRequest req = ctx.bodyAsClass(ReservationRequest.class);

        if (req.roomNumber == 0 || req.guestName == null || req.guestEmail == null || req.guestCpf == null
            || req.checkIn == null || req.checkOut == null || req.guests == 0 || req.ratePlan == null) {
          ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Missing required fields"));
          return;
        }

        LocalDate checkIn, checkOut;
        try {
          checkIn = LocalDate.parse(req.checkIn);
          checkOut = LocalDate.parse(req.checkOut);
        } catch (DateTimeParseException e) {
          ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
          return;
        }

        Reservation.RatePlan ratePlan;
        try {
          ratePlan = Reservation.RatePlan.valueOf(req.ratePlan.toUpperCase());
        } catch (IllegalArgumentException e) {
          ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid rate plan"));
          return;
        }

        Reservation reservation = reservationService.createReservation(
            req.roomNumber, req.guestName, req.guestEmail, req.guestCpf, checkIn, checkOut, req.guests, ratePlan,
            req.companyCode);

        ctx.status(201).json(toJson(reservation));
      } catch (RatePlanStrategy.InvalidReservationException e) {
        if (e.errorCode.equals("ROOM_NOT_FOUND")) {
          ctx.status(404).json(Map.of("error", "ROOM_NOT_FOUND", "message", e.getMessage()));
        } else if (e.errorCode.equals("ROOM_UNAVAILABLE")) {
          ctx.status(409).json(Map.of("error", "ROOM_UNAVAILABLE", "message", e.getMessage()));
        } else if (e.errorCode.equals("CAPACITY_EXCEEDED")) {
          ctx.status(422).json(Map.of("error", "CAPACITY_EXCEEDED", "message", e.getMessage()));
        } else {
          ctx.status(400).json(Map.of("error", e.errorCode, "message", e.getMessage()));
        }
      }
    });

    app.get("/reservations/:id", ctx -> {
      try {
        long id = Long.parseLong(ctx.pathParam("id"));
        Optional<Reservation> reservation = reservationRepository.findById(id);

        if (reservation.isEmpty()) {
          ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", "Reservation not found"));
          return;
        }

        ctx.json(toJson(reservation.get()));
      } catch (NumberFormatException e) {
        ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid reservation ID"));
      }
    });

    app.post("/reservations/:id/confirmation", ctx -> {
      try {
        long id = Long.parseLong(ctx.pathParam("id"));
        String body = ctx.body();

        BigDecimal amountPaid = BigDecimal.ZERO;
        if (body != null && !body.isEmpty()) {
          Map<String, Object> map = objectMapper.readValue(body, Map.class);
          if (map.containsKey("amountPaid")) {
            amountPaid = new BigDecimal(map.get("amountPaid").toString());
          }
        }

        Reservation reservation = reservationService.confirmReservation(id, amountPaid);
        ctx.json(toJson(reservation));
      } catch (NumberFormatException e) {
        ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid reservation ID"));
      } catch (RatePlanStrategy.InvalidReservationException e) {
        if (e.errorCode.equals("RESERVATION_NOT_FOUND")) {
          ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", e.getMessage()));
        } else {
          ctx.status(409).json(Map.of("error", e.errorCode, "message", e.getMessage()));
        }
      }
    });

    app.post("/reservations/:id/cancellation", ctx -> {
      try {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.cancel(id);
        ctx.json(toJson(reservation));
      } catch (NumberFormatException e) {
        ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid reservation ID"));
      } catch (RatePlanStrategy.InvalidReservationException e) {
        if (e.errorCode.equals("RESERVATION_NOT_FOUND")) {
          ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", e.getMessage()));
        } else {
          ctx.status(409).json(Map.of("error", e.errorCode, "message", e.getMessage()));
        }
      }
    });

    app.post("/reservations/:id/check-in", ctx -> {
      try {
        long id = Long.parseLong(ctx.pathParam("id"));
        Reservation reservation = reservationService.checkIn(id);
        ctx.json(toJson(reservation));
      } catch (NumberFormatException e) {
        ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid reservation ID"));
      } catch (RatePlanStrategy.InvalidReservationException e) {
        if (e.errorCode.equals("RESERVATION_NOT_FOUND")) {
          ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", e.getMessage()));
        } else {
          ctx.status(409).json(Map.of("error", e.errorCode, "message", e.getMessage()));
        }
      }
    });

    app.post("/reservations/:id/check-out", ctx -> {
      try {
        long id = Long.parseLong(ctx.pathParam("id"));
        Document document = reservationService.checkOut(id);
        ctx.json(documentToJson(document));
      } catch (NumberFormatException e) {
        ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid reservation ID"));
      } catch (RatePlanStrategy.InvalidReservationException e) {
        if (e.errorCode.equals("RESERVATION_NOT_FOUND")) {
          ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", e.getMessage()));
        } else {
          ctx.status(409).json(Map.of("error", e.errorCode, "message", e.getMessage()));
        }
      }
    });

    app.get("/reservations/:id/document", ctx -> {
      try {
        long id = Long.parseLong(ctx.pathParam("id"));
        Document document = reservationService.getDocument(id);
        ctx.json(documentToJson(document));
      } catch (NumberFormatException e) {
        ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid reservation ID"));
      } catch (RatePlanStrategy.InvalidReservationException e) {
        if (e.errorCode.equals("RESERVATION_NOT_FOUND")) {
          ctx.status(404).json(Map.of("error", "RESERVATION_NOT_FOUND", "message", e.getMessage()));
        } else {
          ctx.status(409).json(Map.of("error", e.errorCode, "message", e.getMessage()));
        }
      }
    });
  }

  private static Map<String, Object> toJson(Reservation r) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", r.getId());
    map.put("roomNumber", r.getRoomNumber());
    map.put("guestName", r.getGuestName());
    map.put("guestEmail", r.getGuestEmail());
    map.put("guestCpf", r.getGuestCpf());
    map.put("checkIn", r.getCheckIn().toString());
    map.put("checkOut", r.getCheckOut().toString());
    map.put("guests", r.getGuests());
    map.put("ratePlan", r.getRatePlan().toString());
    map.put("nights", r.getNights());
    map.put("subtotal", r.getSubtotal());
    map.put("discount", r.getDiscount());
    map.put("total", r.getTotal());
    map.put("status", r.getStatus().toString());
    map.put("amountPaid", r.getAmountPaid());
    map.put("refund", r.getRefund());
    return map;
  }

  private static Map<String, Object> documentToJson(Document d) {
    Map<String, Object> map = new HashMap<>();
    map.put("title", d.getTitle());
    map.put("reservationId", d.getReservationId());
    map.put("guestName", d.getGuestName());
    map.put("guestCpf", d.getGuestCpf());
    map.put("roomNumber", d.getRoomNumber());
    map.put("roomType", d.getRoomType());
    map.put("checkIn", d.getCheckIn().toString());
    map.put("checkOut", d.getCheckOut().toString());
    map.put("nights", d.getNights());
    map.put("subtotal", d.getSubtotal());
    map.put("discount", d.getDiscount());
    map.put("total", d.getTotal());
    return map;
  }
}
