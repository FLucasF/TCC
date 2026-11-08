package com.pousadamarealta.report;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvReportTest {

    @Test
    void shouldGenerateReportWithTitleHeaderDataAndTotal() {
        TestReport report = new TestReport();

        String csv = report.generate();

        String expected = """
            Pousada Maré Alta,test-report,2027-07
            column1,column2
            value1,value2
            value3,value4
            total,2""";

        assertThat(csv).isEqualTo(expected);
    }

    static class TestReport extends CsvReport {
        @Override
        protected String getReportName() {
            return "test-report";
        }

        @Override
        protected String getPeriod() {
            return "2027-07";
        }

        @Override
        protected List<String> getColumnNames() {
            return List.of("column1", "column2");
        }

        @Override
        protected List<List<String>> getDataRows() {
            return List.of(
                List.of("value1", "value2"),
                List.of("value3", "value4")
            );
        }

        @Override
        protected List<String> getTotalRow() {
            return List.of("total", "2");
        }
    }
}
