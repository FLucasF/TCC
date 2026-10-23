package com.pousadamarealta.api.handler;

import com.pousadamarealta.api.dto.ErrorResponse;
import com.pousadamarealta.reports.OccupancyReport;
import com.pousadamarealta.reports.RevenueReport;
import com.pousadamarealta.service.exceptions.InvalidRequestException;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

public class ReportHandler {
    private final OccupancyReport occupancyReport;
    private final RevenueReport revenueReport;

    public ReportHandler(OccupancyReport occupancyReport, RevenueReport revenueReport) {
        this.occupancyReport = occupancyReport;
        this.revenueReport = revenueReport;
    }

    public void getOccupancyReport(Context ctx) {
        try {
            String dateParam = ctx.queryParam("date");
            if (dateParam == null || dateParam.isBlank()) {
                throw new InvalidRequestException("Date parameter is required");
            }

            LocalDate date = LocalDate.parse(dateParam);
            String csv = occupancyReport.generate(date);

            ctx.contentType("text/csv; charset=utf-8")
               .result(csv);

        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid date format"));
        } catch (InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }

    public void getRevenueReport(Context ctx) {
        try {
            String monthParam = ctx.queryParam("month");
            if (monthParam == null || monthParam.isBlank()) {
                throw new InvalidRequestException("Month parameter is required");
            }

            YearMonth month = YearMonth.parse(monthParam);
            String csv = revenueReport.generate(month);

            ctx.contentType("text/csv; charset=utf-8")
               .result(csv);

        } catch (DateTimeParseException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", "Invalid month format"));
        } catch (InvalidRequestException e) {
            ctx.status(400).json(new ErrorResponse("INVALID_REQUEST", e.getMessage()));
        }
    }
}
