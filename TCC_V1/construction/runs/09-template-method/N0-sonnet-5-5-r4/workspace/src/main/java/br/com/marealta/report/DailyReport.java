package br.com.marealta.report;

import br.com.marealta.domain.ErrorCode;
import br.com.marealta.domain.PousadaException;
import br.com.marealta.store.Tx;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

abstract class DailyReport implements Report {
    @Override
    public final String parameter() {
        return "date";
    }

    @Override
    public final ReportTable generate(String period, Tx tx) {
        LocalDate date;
        try {
            date = LocalDate.parse(period);
        } catch (DateTimeParseException e) {
            throw new PousadaException(ErrorCode.INVALID_REQUEST, "date must be formatted as yyyy-MM-dd");
        }
        return build(date, tx);
    }

    protected abstract ReportTable build(LocalDate date, Tx tx);
}
