package br.com.marealta.reservas.reports;

import br.com.marealta.reservas.domain.BookingException;
import br.com.marealta.reservas.domain.ErrorCode;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

final class Periods {
    private Periods() {
    }

    static LocalDate date(String raw) {
        try {
            return LocalDate.parse(required(raw, "date"));
        } catch (DateTimeParseException e) {
            throw new BookingException(ErrorCode.INVALID_REQUEST, "date deve estar no formato AAAA-MM-DD");
        }
    }

    static YearMonth month(String raw) {
        String value = required(raw, "month");
        if (!value.matches("\\d{4}-\\d{2}")) {
            throw new BookingException(ErrorCode.INVALID_REQUEST, "month deve estar no formato AAAA-MM");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new BookingException(ErrorCode.INVALID_REQUEST, "month deve estar no formato AAAA-MM");
        }
    }

    private static String required(String raw, String name) {
        if (raw == null || raw.isBlank()) {
            throw new BookingException(ErrorCode.INVALID_REQUEST, "Parâmetro " + name + " é obrigatório");
        }
        return raw;
    }
}
