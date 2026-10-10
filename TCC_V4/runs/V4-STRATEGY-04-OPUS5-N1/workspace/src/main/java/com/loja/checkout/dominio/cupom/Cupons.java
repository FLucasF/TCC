package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.CodigoErro;
import java.math.BigDecimal;
import java.util.List;

/** Os cupons que valem hoje. */
public final class Cupons {

    public static final Catalogo<Cupom> CATALOGO = Catalogo.de(
            CodigoErro.CUPOM_INVALIDO,
            Cupom::codigo,
            List.of(
                    new PercentualNosProdutos("BEMVINDO10", new BigDecimal("0.10")),
                    new ValorFixoNosProdutos("MENOS50", new BigDecimal("50.00"), new BigDecimal("300.00")),
                    new FreteGratis(),
                    new LeveTresPagueDois()));

    private Cupons() {
    }
}
