package com.pousadamarealta.report;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReportGeneratorTest {

    @Test
    public void testReportStructure() {
        TestReportGenerator report = new TestReportGenerator();
        String csv = report.generate();

        String[] lines = csv.split("\n");

        assertTrue(lines[0].startsWith("Pousada Maré Alta,"));
        assertTrue(lines[1].equals("header"));
        assertTrue(lines[2].equals("data"));
        assertTrue(lines[3].equals("total"));
    }

    private static class TestReportGenerator extends ReportGenerator {
        @Override
        protected String getReportName() {
            return "test";
        }

        @Override
        protected String getPeriod() {
            return "2027-07";
        }

        @Override
        protected String buildHeaderLine() {
            return "header";
        }

        @Override
        protected String buildDataLines() {
            return "data\n";
        }

        @Override
        protected String buildTotalLine() {
            return "total";
        }
    }
}
