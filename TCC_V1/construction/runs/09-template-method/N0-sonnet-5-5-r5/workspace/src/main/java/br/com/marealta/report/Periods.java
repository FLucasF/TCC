package br.com.marealta.report;

import br.com.marealta.domain.DomainException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

final class Periods {
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern MONTH = Pattern.compile("\\d{4}-\\d{2}");

    private Periods() {}

    static LocalDate date(String raw) {
        if (raw == null || !DATE.matcher(raw).matches()) {
            throw DomainException.invalid("Informe a data no formato AAAA-MM-DD");
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw DomainException.invalid("Data inválida: " + raw);
        }
    }

    static YearMonth month(String raw) {
        if (raw == null || !MONTH.matcher(raw).matches()) {
            throw DomainException.invalid("Informe o mês no formato AAAA-MM");
        }
        try {
            return YearMonth.parse(raw);
        } catch (DateTimeParseException e) {
            throw DomainException.invalid("Mês inválido: " + raw);
        }
    }
}
