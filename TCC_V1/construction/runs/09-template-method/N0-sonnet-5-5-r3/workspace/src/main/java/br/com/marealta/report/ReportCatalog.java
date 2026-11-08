package br.com.marealta.report;

import br.com.marealta.db.Database;
import br.com.marealta.service.ApiException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All available reports, and the single path from "report name + period" to CSV text. */
public final class ReportCatalog {

    private final Database db;
    private final Map<String, Report> reports = new LinkedHashMap<>();

    public ReportCatalog(Database db, List<Report> reports) {
        this.db = db;
        reports.forEach(r -> this.reports.put(r.name(), r));
    }

    public List<Report> all() {
        return List.copyOf(reports.values());
    }

    /** Generates the CSV of a report; {@code rawPeriod} is the query value as received. */
    public String csv(Report report, String rawPeriod) {
        String period = report.periodKind().normalize(rawPeriod);
        ReportTable table = db.transaction(c -> report.build(c, period));
        return ReportRenderer.render(table);
    }

    public Report get(String name) {
        Report r = reports.get(name);
        if (r == null) throw new ApiException(404, "REPORT_NOT_FOUND", "Relatório " + name + " não existe");
        return r;
    }
}
