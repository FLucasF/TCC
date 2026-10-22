package br.com.marealta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Parte em português dos documentos: rótulos, dinheiro R$ e datas dd/MM/yyyy. */
public abstract class PortugueseDocumentFormat extends DocumentFormat {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    protected String numberMarker() { return "nº"; }

    protected String roomLabel() { return "Quarto"; }

    protected String checkInLabel() { return "Entrada"; }

    protected String checkOutLabel() { return "Saída"; }

    protected String nightsText(int nights) { return nights + (nights == 1 ? " diária" : " diárias"); }

    protected String subtotalLabel() { return "Diárias"; }

    protected String discountLabel() { return "Desconto"; }

    protected String totalLabel() { return "Total"; }

    protected String money(BigDecimal value) { return Money.brazilian(value); }

    protected String date(LocalDate date) { return DATE.format(date); }
}
