package pousada.report;

import java.util.List;

public interface Report {
    String getName();
    String getPeriod();
    List<String> getColumns();
    List<Row> getDataRows();
    Row getTotalRow();

    record Row(List<String> values) {}
}
