package br.com.mare.pousada.report;

import br.com.mare.pousada.domain.BusinessException;
import br.com.mare.pousada.domain.ErrorCode;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Todo relatório sai por aqui: título, cabeçalho, linhas, total. Mudar o jeito dos relatórios
 * (por exemplo, uma linha final com a data em que foi tirado) é mudar só {@link #render}.
 * Cada relatório define apenas o nome, o parâmetro e o conteúdo.
 *
 * @param <P> o período do relatório (uma noite, um mês)
 */
public abstract class Report<P> {
    static final String ESTABLISHMENT = "Pousada Maré Alta";
    static final DateTimeFormatter STRICT_DATE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter STRICT_MONTH =
            DateTimeFormatter.ofPattern("uuuu-MM").withResolverStyle(ResolverStyle.STRICT);

    /** Nome no título e no endereço: occupancy, revenue... */
    public abstract String name();

    /** Nome do parâmetro de consulta: date, month. */
    public abstract String parameter();

    protected abstract P parse(String raw);

    protected abstract Table build(Connection connection, P period) throws SQLException;

    public final String render(Connection connection, String rawPeriod) throws SQLException {
        if (rawPeriod == null) {
            throw invalid("Falta o parâmetro " + parameter());
        }
        P period = parse(rawPeriod);
        Table table = build(connection, period);
        List<List<String>> lines = new ArrayList<>();
        lines.add(List.of(ESTABLISHMENT, name(), period.toString()));
        lines.add(table.header());
        lines.addAll(table.rows());
        lines.add(table.total());
        return lines.stream().map(Report::csvLine).collect(Collectors.joining("\n", "", "\n"));
    }

    protected static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, message);
    }

    private static String csvLine(List<String> cells) {
        return cells.stream().map(Report::csvCell).collect(Collectors.joining(","));
    }

    private static String csvCell(String cell) {
        boolean needsQuotes = cell.contains(",") || cell.contains("\"") || cell.contains("\n");
        return needsQuotes ? '"' + cell.replace("\"", "\"\"") + '"' : cell;
    }
}
