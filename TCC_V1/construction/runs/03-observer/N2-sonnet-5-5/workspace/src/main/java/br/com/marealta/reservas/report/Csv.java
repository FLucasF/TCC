package br.com.marealta.reservas.report;

import java.math.BigDecimal;

final class Csv {
    private Csv() {}

    static String money(BigDecimal value) {
        return value.toPlainString();
    }
}
