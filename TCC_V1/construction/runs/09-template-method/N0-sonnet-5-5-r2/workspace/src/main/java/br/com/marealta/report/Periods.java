package br.com.marealta.report;

import br.com.marealta.service.ApiException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class Periods {
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");

    private Periods() {}

    public static LocalDate date(String raw) {
        if (raw != null && DATE.matcher(raw).matches()) {
            try {
                return LocalDate.parse(raw);
            } catch (DateTimeParseException ignored) { }
        }
        throw ApiException.invalid("Data inválida (use aaaa-mm-dd)");
    }

    public static YearMonth month(String raw) {
        if (raw != null && MONTH.matcher(raw).matches()) {
            try {
                return YearMonth.parse(raw);
            } catch (DateTimeParseException ignored) { }
        }
        throw ApiException.invalid("Mês inválido (use aaaa-mm)");
    }
}
