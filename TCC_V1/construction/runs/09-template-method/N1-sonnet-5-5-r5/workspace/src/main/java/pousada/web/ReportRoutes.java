package pousada.web;

import java.util.List;

import io.javalin.Javalin;
import pousada.report.CsvReportWriter;
import pousada.report.Report;

final class ReportRoutes {
    private ReportRoutes() {
    }

    static void register(Javalin app, List<Report> reports, CsvReportWriter writer) {
        for (Report report : reports) {
            app.get("/reports/" + report.name(), ctx -> {
                String csv = writer.write(report, report.generate(ctx.queryParam(report.parameter())));
                ctx.contentType("text/csv; charset=utf-8").result(csv);
            });
        }
    }
}
