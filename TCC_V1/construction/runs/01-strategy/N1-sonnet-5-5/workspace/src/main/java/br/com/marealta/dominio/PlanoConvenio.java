package br.com.marealta.dominio;

import java.math.BigDecimal;
import java.util.Map;

public class PlanoConvenio extends PlanoDeTarifa {

    private static final Map<String, Integer> DESCONTO_POR_EMPRESA = Map.of(
            "TECNOSUL", 12,
            "AGROVALE", 18,
            "FARMAVIDA", 25);

    public PlanoConvenio() {
        super("CORPORATE");
    }

    @Override
    protected void validar(Estadia estadia) {
        String empresa = estadia.codigoEmpresa();
        if (empresa == null || !DESCONTO_POR_EMPRESA.containsKey(empresa)) {
            throw ApiException.invalida("Código de empresa ausente ou sem convênio");
        }
    }

    @Override
    protected BigDecimal desconto(Estadia estadia, BigDecimal diarias) {
        return Dinheiro.percentual(diarias, DESCONTO_POR_EMPRESA.get(estadia.codigoEmpresa()));
    }
}
