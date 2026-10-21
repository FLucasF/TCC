package br.com.marealta.dominio;

import java.math.BigDecimal;

public class PlanoEstadiaLonga extends PlanoDeTarifa {

    private static final int MINIMO_DE_NOITES = 7;

    public PlanoEstadiaLonga() {
        super("LONG_STAY");
    }

    @Override
    protected void validar(Estadia estadia) {
        if (estadia.noites() < MINIMO_DE_NOITES) {
            throw ApiException.invalida("A estadia longa exige pelo menos " + MINIMO_DE_NOITES + " noites");
        }
    }

    @Override
    protected BigDecimal desconto(Estadia estadia, BigDecimal diarias) {
        return Dinheiro.percentual(diarias, 10);
    }
}
