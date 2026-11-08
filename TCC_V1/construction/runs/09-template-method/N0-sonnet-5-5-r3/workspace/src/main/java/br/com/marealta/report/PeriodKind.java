package br.com.marealta.report;

import br.com.marealta.service.ApiException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.regex.Pattern;

/** What period a report covers, and how it is given in the query string. */
public enum PeriodKind {
    DATE("date", "\\d{4}-\\d{2}-\\d{2}"),
    MONTH("month", "\\d{4}-\\d{2}");

    private final String parameter;
    private final Pattern format;

    PeriodKind(String parameter, String format) {
        this.parameter = parameter;
        this.format = Pattern.compile(format);
    }

    public String parameter() { return parameter; }

    /** Checks the raw query value and returns it in canonical form (2027-07-10 or 2027-07). */
    public String normalize(String raw) {
        if (raw == null || !format.matcher(raw).matches()) {
            throw ApiException.invalid("Parâmetro '" + parameter + "' ausente ou em formato inválido");
        }
        try {
            return this == DATE ? LocalDate.parse(raw).toString() : YearMonth.parse(raw).toString();
        } catch (java.time.DateTimeException e) {
            throw ApiException.invalid("Parâmetro '" + parameter + "' inválido: " + raw);
        }
    }

    public static LocalDate date(String period) { return LocalDate.parse(period); }

    public static YearMonth month(String period) { return YearMonth.parse(period); }
}
