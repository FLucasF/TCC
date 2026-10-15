package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Registro;

/** Os cupons que valem hoje. */
public final class Cupons {

    public static final Registro<Cupom> REGISTRO = new Registro<>(
            new Bemvindo10(),
            new Menos50(),
            new FreteGratis(),
            new Leve3Pague2());

    private Cupons() {
    }
}
