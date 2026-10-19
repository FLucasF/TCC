package br.com.mareaalta.api;

import br.com.mareaalta.service.ReservationService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.YearMonth;

public class ReportController {
    private final ReservationService reservationService;

    public ReportController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    public void getOccupancyReport(Context ctx) {
        String dateParam = ctx.queryParam("date");
        if (dateParam == null || dateParam.isBlank()) {
            throw new IllegalArgumentException("Query parameter 'date' is required");
        }

        LocalDate date = ApiMapper.parseDate(dateParam);
        String csv = reservationService.generateOccupancyReport(date);

        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }

    public void getRevenueReport(Context ctx) {
        String monthParam = ctx.queryParam("month");
        if (monthParam == null || monthParam.isBlank()) {
            throw new IllegalArgumentException("Query parameter 'month' is required");
        }

        YearMonth month = ApiMapper.parseMonth(monthParam);
        String csv = reservationService.generateRevenueReport(month);

        ctx.contentType("text/csv; charset=utf-8");
        ctx.result(csv);
    }
}
