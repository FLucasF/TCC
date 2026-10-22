package com.pousada.api;

import com.pousada.database.ReservationRepository;
import com.pousada.database.RoomRepository;
import com.pousada.model.Reservation;
import com.pousada.model.Room;
import com.pousada.service.FormattingService;
import io.javalin.http.Context;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

public class ReportsEndpoint {
  public static void getOccupancy(Context ctx) {
    try {
      String dateStr = ctx.queryParam("date");
      if (dateStr == null || dateStr.isEmpty()) {
        ctx.status(400).json(errorResponse("INVALID_REQUEST", "date parameter required"));
        return;
      }

      LocalDate date = LocalDate.parse(dateStr);
      List<Reservation> reservations = ReservationRepository.findConfirmedByDate(date);
      List<Room> rooms = RoomRepository.findAll();

      StringBuilder csv = new StringBuilder();
      csv.append("room,type,reservation,check_in,check_out\n");

      int occupiedCount = 0;
      for (Room room : rooms) {
        for (Reservation res : reservations) {
          if (res.roomNumber == room.number) {
            csv.append(room.number)
                .append(",")
                .append(room.type)
                .append(",")
                .append(res.id)
                .append(",")
                .append(FormattingService.formatCsvDate(res.checkIn))
                .append(",")
                .append(FormattingService.formatCsvDate(res.checkOut))
                .append("\n");
            occupiedCount++;
            break;
          }
        }
      }

      csv.append("total,").append(occupiedCount).append("/").append(rooms.size());

      ctx.contentType("text/csv; charset=utf-8").result(csv.toString());
    } catch (DateTimeParseException e) {
      ctx.status(400).json(errorResponse("INVALID_REQUEST", "Invalid date format"));
    }
  }

  public static void getRevenue(Context ctx) {
    try {
      String monthStr = ctx.queryParam("month");
      if (monthStr == null || monthStr.isEmpty()) {
        ctx.status(400).json(errorResponse("INVALID_REQUEST", "month parameter required"));
        return;
      }

      YearMonth month = YearMonth.parse(monthStr);
      List<Reservation> reservations =
          ReservationRepository.findByMonthAndNonCancelled(month.getYear(), month.getMonthValue());

      StringBuilder csv = new StringBuilder();
      csv.append("reservation,room,check_in,check_out,status,amount\n");

      BigDecimal total = BigDecimal.ZERO;

      for (Reservation res : reservations) {
        BigDecimal amount = res.amountPaid;
        if (res.refund != null) {
          amount = amount.subtract(res.refund);
        }

        csv.append(res.id)
            .append(",")
            .append(res.roomNumber)
            .append(",")
            .append(FormattingService.formatCsvDate(res.checkIn))
            .append(",")
            .append(FormattingService.formatCsvDate(res.checkOut))
            .append(",")
            .append(res.status)
            .append(",")
            .append(FormattingService.formatCsv(amount))
            .append("\n");

        total = total.add(amount);
      }

      csv.append("total,,,,").append(FormattingService.formatCsv(total));

      ctx.contentType("text/csv; charset=utf-8").result(csv.toString());
    } catch (DateTimeParseException e) {
      ctx.status(400).json(errorResponse("INVALID_REQUEST", "Invalid month format"));
    }
  }

  private static java.util.Map<String, String> errorResponse(String code, String message) {
    return java.util.Map.of("error", code, "message", message);
  }
}
