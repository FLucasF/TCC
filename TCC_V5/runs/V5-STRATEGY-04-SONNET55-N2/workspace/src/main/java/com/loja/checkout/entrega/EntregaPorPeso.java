package com.loja.checkout.entrega;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.Pedido;
import java.math.BigDecimal;

abstract class EntregaPorPeso implements Entrega {
    private final BigDecimal base;
    private final BigDecimal porKg;

    EntregaPorPeso(String base, String porKg) {
        this.base = new BigDecimal(base);
        this.porKg = new BigDecimal(porKg);
    }

    @Override
    public boolean atende(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(base.add(porKg.multiply(pedido.pesoKg())));
    }
}
