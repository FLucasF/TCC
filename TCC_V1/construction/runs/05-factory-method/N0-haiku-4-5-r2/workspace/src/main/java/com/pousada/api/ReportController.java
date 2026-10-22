package com.pousada.api;

import io.javalin.Javalin;
import com.pousada.db.Database;
import com.pousada.service.ReportService;
import java.time.LocalDate;
import java.util.Map;

public class ReportController {
    public static void setup(Javalin app, Database db) {
        ReportService service = new ReportService(db);

        app.get("/reports/occupancy", ctx -> {
            try {
                String dateStr = ctx.queryParam("date");
                if (dateStr == null || dateStr.isEmpty()) {
                    ctx.status(400).json(Map.of(
                        "error", "INVALID_REQUEST",
                        "message", "Parâmetro 'date' obrigatório"
                    ));
                    return;
                }

                LocalDate date = LocalDate.parse(dateStr);
                String csv = service.getOccupancyReport(date);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (Exception e) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Data em formato inválido"
                ));
            }
        });

        app.get("/reports/revenue", ctx -> {
            try {
                String monthStr = ctx.queryParam("month");
                if (monthStr == null || monthStr.isEmpty()) {
                    ctx.status(400).json(Map.of(
                        "error", "INVALID_REQUEST",
                        "message", "Parâmetro 'month' obrigatório"
                    ));
                    return;
                }

                String[] parts = monthStr.split("-");
                if (parts.length != 2) {
                    throw new Exception("Invalid format");
                }
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);

                String csv = service.getRevenueReport(year, month);
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            } catch (Exception e) {
                ctx.status(400).json(Map.of(
                    "error", "INVALID_REQUEST",
                    "message", "Mês em formato inválido"
                ));
            }
        });
    }
}
