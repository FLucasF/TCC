package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/** Base das modalidades que cobram uma parte fixa mais um valor por kg. */
abstract class EntregaPorPeso implements ModalidadeEntrega {

    private final BigDecimal fixo;
    private final BigDecimal porKg;

    EntregaPorPeso(String fixo, String porKg) {
        this.fixo = new BigDecimal(fixo);
        this.porKg = new BigDecimal(porKg);
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.centavos(fixo.add(porKg.multiply(pesoKg)));
    }
}
