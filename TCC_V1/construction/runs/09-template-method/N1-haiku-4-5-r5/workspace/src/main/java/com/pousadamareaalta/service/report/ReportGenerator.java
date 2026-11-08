package com.pousadamareaalta.service.report;

import java.sql.SQLException;

public interface ReportGenerator {
    String generate() throws SQLException;
    String getReportName();
}
