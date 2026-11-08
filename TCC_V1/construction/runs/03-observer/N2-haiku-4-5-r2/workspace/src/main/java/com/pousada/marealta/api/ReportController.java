package com.pousada.marealta.api;

import com.pousada.marealta.service.ReportService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ReportController {
  private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

  public static void getOccupancyReport(Context ctx) {
    try {
      String dateStr = ctx.queryParam("date");
      if (dateStr == null || dateStr.isEmpty()) {
        ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Date parameter is required"));
        return;
      }

      LocalDate date = parseDate(dateStr);
      if (date == null) {
        ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format (use yyyy-MM-dd)"));
        return;
      }

      String csv = ReportService.generateOccupancyReport(date);
      ctx.contentType("text/csv; charset=utf-8").result(csv);
    } catch (Exception e) {
      logger.error("Error generating occupancy report", e);
      ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Failed to generate report"));
    }
  }

  public static void getRevenueReport(Context ctx) {
    try {
      String monthStr = ctx.queryParam("month");
      if (monthStr == null || monthStr.isEmpty()) {
        ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Month parameter is required"));
        return;
      }

      if (!monthStr.matches("\\d{4}-\\d{2}")) {
        ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format (use yyyy-MM)"));
        return;
      }

      String csv = ReportService.generateRevenueReport(monthStr);
      ctx.contentType("text/csv; charset=utf-8").result(csv);
    } catch (Exception e) {
      logger.error("Error generating revenue report", e);
      ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Failed to generate report"));
    }
  }

  private static LocalDate parseDate(String dateStr) {
    try {
      return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    } catch (Exception e) {
      return null;
    }
  }
}
