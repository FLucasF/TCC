package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Registro;

import java.util.List;
import java.util.Optional;

/** Os cupons que valem hoje. */
public final class Cupons {

    private static final List<Cupom> CASOS = List.of(
            new PercentualProdutos("BEMVINDO10", "0.10"),
            new ValorFixoComMinimo("MENOS50", "50.00", "300.00"),
            new FreteGratis(),
            new Leve3Pague2());

    private Cupons() {
    }

    public static Optional<Cupom> porCodigo(String codigo) {
        return Registro.busca(CASOS, Cupom::codigo, codigo);
    }
}
