package br.com.mare.report;

import br.com.mare.domain.BusinessException;
import br.com.mare.domain.ErrorCode;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Relatório de um mês (?month=2027-07). */
public abstract class MonthlyReport extends Report<YearMonth> {

    @Override
    public String parameter() {
        return "month";
    }

    @Override
    protected YearMonth parse(String raw) {
        try {
            if (raw == null || !raw.matches("\\d{4}-\\d{2}")) {
                throw new DateTimeParseException("formato", String.valueOf(raw), 0);
            }
            return YearMonth.parse(raw);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "month deve estar no formato 2027-07");
        }
    }

    @Override
    protected String periodLabel(YearMonth month) {
        return month.toString();
    }
}
