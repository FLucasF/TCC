package pousada.report;

import pousada.ApiException;

/**
 * Um relatório declara o nome, o parâmetro de período e como montar suas linhas.
 * A sequência (ler o período, montar, formatar) é a mesma para todos e fica aqui.
 */
public abstract class Report<P> {
    private final String name;
    private final String periodParam;

    protected Report(String name, String periodParam) {
        this.name = name;
        this.periodParam = periodParam;
    }

    public final String name() {
        return name;
    }

    /** Nome do parâmetro de query que traz o período (date, month...). */
    public final String periodParam() {
        return periodParam;
    }

    public final String generate(String rawPeriod) {
        if (rawPeriod == null) {
            throw ApiException.invalidRequest("Parâmetro obrigatório: " + periodParam);
        }
        P period;
        try {
            period = parse(rawPeriod);
        } catch (RuntimeException e) {
            throw ApiException.invalidRequest("Valor inválido para " + periodParam + ": " + rawPeriod);
        }
        return ReportRenderer.render(name, period.toString(), build(period));
    }

    protected abstract P parse(String raw);

    protected abstract ReportData build(P period);
}
