package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/** Entrega que cobra uma taxa fixa mais um valor por kg do pedido. */
abstract class EntregaPorPeso implements ModalidadeEntrega {

    private final BigDecimal fixo;
    private final BigDecimal porKg;

    EntregaPorPeso(String fixo, String porKg) {
        this.fixo = new BigDecimal(fixo);
        this.porKg = new BigDecimal(porKg);
    }

    @Override
    public BigDecimal frete(BigDecimal pesoKg) {
        return Dinheiro.arredonda(fixo.add(porKg.multiply(pesoKg)));
    }
}
