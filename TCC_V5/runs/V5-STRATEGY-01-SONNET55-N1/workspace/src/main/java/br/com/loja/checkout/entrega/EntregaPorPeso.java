package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.Pedido;

import java.math.BigDecimal;

abstract class EntregaPorPeso implements Entrega {

    private final BigDecimal base;
    private final BigDecimal porKg;

    EntregaPorPeso(String base, String porKg) {
        this.base = new BigDecimal(base);
        this.porKg = new BigDecimal(porKg);
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(base.add(porKg.multiply(pedido.pesoKg())));
    }
}
