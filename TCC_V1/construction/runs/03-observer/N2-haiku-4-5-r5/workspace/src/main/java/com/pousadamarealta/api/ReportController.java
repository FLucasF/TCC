package com.pousadamarealta.api;

import com.pousadamarealta.services.ReportService;
import io.javalin.http.Context;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportController {
  private static final Logger log = LoggerFactory.getLogger(ReportController.class);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public static void getOccupancy(Context ctx) {
    try {
      String dateStr = ctx.queryParam("date");
      if (dateStr == null) {
        throw new Exception("INVALID_REQUEST");
      }

      LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
      String csv = ReportService.getOccupancyReport(date);

      ctx.contentType("text/csv; charset=utf-8");
      ctx.result(csv);
    } catch (Exception e) {
      handleException(ctx, e);
    }
  }

  public static void getRevenue(Context ctx) {
    try {
      String monthStr = ctx.queryParam("month");
      if (monthStr == null) {
        throw new Exception("INVALID_REQUEST");
      }

      String csv = ReportService.getRevenueReport(monthStr);

      ctx.contentType("text/csv; charset=utf-8");
      ctx.result(csv);
    } catch (Exception e) {
      handleException(ctx, e);
    }
  }

  private static void handleException(Context ctx, Exception e) {
    String errorCode = e.getMessage();
    int status = 400;
    String message = "";

    switch (errorCode) {
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
    ctx.json(new java.util.HashMap<>(java.util.Map.of("error", errorCode, "message", message)));
  }
}
