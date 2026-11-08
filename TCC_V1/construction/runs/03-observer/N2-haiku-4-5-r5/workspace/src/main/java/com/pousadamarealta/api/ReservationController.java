package com.pousadamarealta.api;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.services.ReservationService;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReservationController {
  private static final Logger log = LoggerFactory.getLogger(ReservationController.class);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public static void createReservation(Context ctx) {
    try {
      var body = ctx.bodyAsClass(Map.class);

      Integer roomNumber = ((Number) body.get("roomNumber")).intValue();
      String guestName = (String) body.get("guestName");
      String guestEmail = (String) body.get("guestEmail");
      String guestCpf = (String) body.get("guestCpf");
      String checkInStr = (String) body.get("checkIn");
      String checkOutStr = (String) body.get("checkOut");
      Integer guests = ((Number) body.get("guests")).intValue();
      String ratePlanStr = (String) body.get("ratePlan");

      LocalDate checkIn = LocalDate.parse(checkInStr, DATE_FORMATTER);
      LocalDate checkOut = LocalDate.parse(checkOutStr, DATE_FORMATTER);
      RatePlan ratePlan = RatePlan.from(ratePlanStr);

      Reservation reservation = ReservationService.create(
          roomNumber,
          guestName,
          guestEmail,
          guestCpf,
          checkIn,
          checkOut,
          guests,
          ratePlan);

      ctx.status(201);
      ctx.json(buildReservationDto(reservation));
    } catch (Exception e) {
      handleException(ctx, e);
    }
  }

  public static void getReservation(Context ctx) {
    try {
      long id = Long.parseLong(ctx.pathParam("id"));
      var reservation = com.pousadamarealta.infrastructure.ReservationRepository.findById(id)
          .orElseThrow(() -> new Exception("RESERVATION_NOT_FOUND"));
      ctx.json(buildReservationDto(reservation));
    } catch (Exception e) {
      handleException(ctx, e);
    }
  }

  public static void confirmReservation(Context ctx) {
    try {
      long id = Long.parseLong(ctx.pathParam("id"));
      var body = ctx.bodyAsClass(Map.class);
      BigDecimal amountPaid = BigDecimal.valueOf(((Number) body.get("amountPaid")).doubleValue());

      Reservation reservation = ReservationService.confirm(id, amountPaid);
      ctx.json(buildReservationDto(reservation));
    } catch (Exception e) {
      handleException(ctx, e);
    }
  }

  public static void cancelReservation(Context ctx) {
    try {
      long id = Long.parseLong(ctx.pathParam("id"));
      Reservation reservation = ReservationService.cancel(id);
      ctx.json(buildReservationDto(reservation));
    } catch (Exception e) {
      handleException(ctx, e);
    }
  }

  public static void checkInReservation(Context ctx) {
    try {
      long id = Long.parseLong(ctx.pathParam("id"));
      Reservation reservation = ReservationService.checkIn(id);
      ctx.json(buildReservationDto(reservation));
    } catch (Exception e) {
      handleException(ctx, e);
    }
  }

  public static void checkOutReservation(Context ctx) {
    try {
      long id = Long.parseLong(ctx.pathParam("id"));
      Reservation reservation = ReservationService.checkOut(id);
      ctx.json(buildDocumentDto(reservation));
    } catch (Exception e) {
      handleException(ctx, e);
    }
  }

  public static void getDocument(Context ctx) {
    try {
      long id = Long.parseLong(ctx.pathParam("id"));
      var reservation = com.pousadamarealta.infrastructure.ReservationRepository.findById(id)
          .orElseThrow(() -> new Exception("RESERVATION_NOT_FOUND"));

      if (reservation.status.name().equals("PENDING")) {
        throw new Exception("OPERATION_NOT_ALLOWED");
      }

      ctx.json(buildDocumentDto(reservation));
    } catch (Exception e) {
      handleException(ctx, e);
    }
  }

  private static Map<String, Object> buildReservationDto(Reservation res) {
    Map<String, Object> dto = new HashMap<>();
    dto.put("id", res.id);
    dto.put("roomNumber", res.roomNumber);
    dto.put("guestName", res.guestName);
    dto.put("guestEmail", res.guestEmail);
    dto.put("guestCpf", res.guestCpf);
    dto.put("checkIn", res.checkIn.format(DATE_FORMATTER));
    dto.put("checkOut", res.checkOut.format(DATE_FORMATTER));
    dto.put("guests", res.guests);
    dto.put("ratePlan", res.ratePlan.value);
    dto.put("nights", res.nights);
    dto.put("subtotal", res.subtotal);
    dto.put("discount", res.discount);
    dto.put("total", res.total);
    dto.put("status", res.status.value);
    dto.put("amountPaid", res.amountPaid);
    dto.put("refund", res.refund);
    return dto;
  }

  private static Map<String, Object> buildDocumentDto(Reservation res) throws Exception {
    var room = com.pousadamarealta.infrastructure.RoomRepository.findById(res.roomNumber);
    if (room.isEmpty()) {
      throw new RuntimeException("Room not found");
    }

    Map<String, Object> dto = new HashMap<>();
    dto.put("title", "Comprovante de reserva");
    dto.put("reservationId", res.id);
    dto.put("guestName", res.guestName);
    dto.put("guestCpf", res.guestCpf);
    dto.put("roomNumber", res.roomNumber);
    dto.put("roomType", room.get().type.value);
    dto.put("checkIn", res.checkIn.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    dto.put("checkOut", res.checkOut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    dto.put("nights", res.nights);
    dto.put("subtotal", res.subtotal);
    dto.put("discount", res.discount);
    dto.put("total", res.total);
    return dto;
  }

  private static void handleException(Context ctx, Exception e) {
    String errorCode = e.getMessage();
    int status = 400;
    String message = "";

    switch (errorCode) {
      case "ROOM_NOT_FOUND":
        status = 404;
        message = "Quarto não encontrado";
        break;
      case "RESERVATION_NOT_FOUND":
        status = 404;
        message = "Reserva não encontrada";
        break;
      case "ROOM_UNAVAILABLE":
        status = 409;
        message = "Quarto indisponível para as datas solicitadas";
        break;
      case "OPERATION_NOT_ALLOWED":
        status = 409;
        message = "Operação não permitida para este estado de reserva";
        break;
      case "CAPACITY_EXCEEDED":
        status = 422;
        message = "Número de hóspedes excede a capacidade do quarto";
        break;
      case "INVALID_REQUEST":
        status = 400;
        message = "Requisição inválida";
        break;
      default:
        log.error("Unexpected error", e);
        status = 500;
        message = "Erro interno do servidor";
    }

    ctx.status(status);
    Map<String, Object> error = new HashMap<>();
    error.put("error", errorCode);
    error.put("message", message);
    ctx.json(error);
  }
}
