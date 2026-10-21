package br.com.marealta.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Os dados de uma estadia que os planos de tarifa precisam para calcular e validar. */
public record Estadia(Quarto quarto, LocalDate entrada, LocalDate saida, String codigoEmpresa) {

    public int noites() {
        return (int) ChronoUnit.DAYS.between(entrada, saida);
    }

    /** Soma das noites ao preço de tabela, com a alta temporada, arredondando noite a noite. */
    public BigDecimal somaDasNoites() {
        BigDecimal soma = Dinheiro.ZERO;
        for (LocalDate noite = entrada; noite.isBefore(saida); noite = noite.plusDays(1)) {
            soma = soma.add(precoDaNoite(noite));
        }
        return soma;
    }

    private BigDecimal precoDaNoite(LocalDate noite) {
        if (Temporada.alta(noite)) {
            return Dinheiro.percentual(quarto.diaria(), 120);
        }
        return quarto.diaria();
    }
}
