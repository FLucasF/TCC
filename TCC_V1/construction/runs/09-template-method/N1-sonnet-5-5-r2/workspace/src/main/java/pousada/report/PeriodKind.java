package pousada.report;

import pousada.domain.ApiException;
import pousada.domain.ErrorCode;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** O tipo de período que um relatório pede na URL: uma noite ou um mês. */
public enum PeriodKind {
    DAY("date") {
        @Override
        Period parse(String text) throws DateTimeParseException {
            LocalDate date = LocalDate.parse(text);
            return new Period(date.toString(), date, date.plusDays(1));
        }
    },
    MONTH("month") {
        @Override
        Period parse(String text) throws DateTimeParseException {
            YearMonth month = YearMonth.parse(text);
            return new Period(month.toString(), month.atDay(1), month.plusMonths(1).atDay(1));
        }
    };

    private final String parameter;

    PeriodKind(String parameter) {
        this.parameter = parameter;
    }

    public String parameter() {
        return parameter;
    }

    abstract Period parse(String text) throws DateTimeParseException;

    public Period read(String text) {
        if (text == null) {
            throw invalid("Informe o parâmetro " + parameter);
        }
        try {
            return parse(text);
        } catch (DateTimeParseException e) {
            throw invalid("Valor inválido para " + parameter + ": " + text);
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(ErrorCode.INVALID_REQUEST, message);
    }
}
