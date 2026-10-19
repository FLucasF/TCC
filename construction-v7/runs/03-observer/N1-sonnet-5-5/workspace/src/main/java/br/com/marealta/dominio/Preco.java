package br.com.marealta.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resultado do cálculo de uma estadia. */
public record Preco(int noites, BigDecimal subtotal, BigDecimal desconto, BigDecimal total) {

    private static final BigDecimal ACRESCIMO_ALTA_TEMPORADA = new BigDecimal("1.20");

    public static Preco calcular(Quarto quarto, LocalDate entrada, LocalDate saida, PlanoTarifa plano) {
        BigDecimal subtotal = Dinheiro.ZERO;
        int noites = 0;
        for (LocalDate noite = entrada; noite.isBefore(saida); noite = noite.plusDays(1)) {
            subtotal = subtotal.add(precoDaNoite(quarto, noite));
            noites++;
        }
        BigDecimal desconto = plano.descontoSobre(subtotal);
        return new Preco(noites, subtotal, desconto, Dinheiro.arredondar(subtotal.subtract(desconto)));
    }

    private static BigDecimal precoDaNoite(Quarto quarto, LocalDate noite) {
        BigDecimal diaria = quarto.diaria();
        return Dinheiro.arredondar(altaTemporada(noite) ? diaria.multiply(ACRESCIMO_ALTA_TEMPORADA) : diaria);
    }

    /** De 15 de dezembro ao fim de fevereiro, e julho inteiro. */
    static boolean altaTemporada(LocalDate noite) {
        int mes = noite.getMonthValue();
        return mes == 1 || mes == 2 || mes == 7 || (mes == 12 && noite.getDayOfMonth() >= 15);
    }
}
