package br.com.marealta.dominio;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Map;

public class PlanoFimDeSemana extends PlanoDeTarifa {

    private static final Map<TipoQuarto, BigDecimal> PRECO_FECHADO = Map.of(
            TipoQuarto.STANDARD, Dinheiro.valor("520.00"),
            TipoQuarto.DELUXE, Dinheiro.valor("820.00"),
            TipoQuarto.SUITE, Dinheiro.valor("1200.00"));

    public PlanoFimDeSemana() {
        super("WEEKEND_PACKAGE");
    }

    @Override
    protected void validar(Estadia estadia) {
        boolean sextaADomingo = estadia.entrada().getDayOfWeek() == DayOfWeek.FRIDAY
                && estadia.noites() == 2;
        if (!sextaADomingo) {
            throw ApiException.invalida("O pacote de fim de semana vale de sexta a domingo, com duas noites");
        }
    }

    @Override
    protected BigDecimal diarias(Estadia estadia) {
        return PRECO_FECHADO.get(estadia.quarto().tipo());
    }

    @Override
    public BigDecimal devolucao(BigDecimal valorPago) {
        return Dinheiro.percentual(valorPago, 50);
    }
}
