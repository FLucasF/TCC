package br.com.mare.pousada.report;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Relatório de um mês, pedido por ?month=2027-07. */
public abstract class MonthlyReport extends Report<YearMonth> {

    @Override
    public String parameter() {
        return "month";
    }

    @Override
    protected YearMonth parse(String raw) {
        try {
            return YearMonth.parse(raw, STRICT_MONTH);
        } catch (DateTimeParseException e) {
            throw invalid("Mês inválido: " + raw + " (use 2027-07)");
        }
    }
}
