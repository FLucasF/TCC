package com.pousada.api;

import io.javalin.Javalin;
import com.pousada.repository.ReservationRepository;
import com.pousada.repository.RoomRepository;
import com.pousada.service.ReportService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Map;

public class ReportController {
  private static final ReservationRepository reservationRepository = new ReservationRepository();
  private static final RoomRepository roomRepository = new RoomRepository();
  private static final ReportService reportService = new ReportService(reservationRepository, roomRepository);

  public static void routes(Javalin app) {
    app.get("/reports/occupancy", ctx -> {
      String dateParam = ctx.queryParam("date");
      if (dateParam == null || dateParam.isEmpty()) {
        ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Date parameter is required"));
        return;
      }

      try {
        LocalDate date = LocalDate.parse(dateParam);
        String report = reportService.occupancyReport(date);
        ctx.contentType("text/csv").result(report);
      } catch (DateTimeParseException e) {
        ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid date format"));
      }
    });

    app.get("/reports/revenue", ctx -> {
      String monthParam = ctx.queryParam("month");
      if (monthParam == null || monthParam.isEmpty()) {
        ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Month parameter is required"));
        return;
      }

      try {
        YearMonth yearMonth = YearMonth.parse(monthParam);
        String report = reportService.revenueReport(yearMonth.getYear(), yearMonth.getMonthValue());
        ctx.contentType("text/csv").result(report);
      } catch (DateTimeParseException e) {
        ctx.status(400).json(Map.of("error", "INVALID_REQUEST", "message", "Invalid month format"));
      }
    });
  }
}
