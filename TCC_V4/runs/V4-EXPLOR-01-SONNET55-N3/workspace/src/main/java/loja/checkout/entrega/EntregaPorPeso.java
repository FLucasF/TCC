package loja.checkout.entrega;

import java.math.BigDecimal;

import loja.checkout.comum.Compra;
import loja.checkout.comum.Dinheiro;

abstract class EntregaPorPeso implements Entrega {
    private final BigDecimal base;
    private final BigDecimal porKg;

    EntregaPorPeso(String base, String porKg) {
        this.base = new BigDecimal(base);
        this.porKg = new BigDecimal(porKg);
    }

    @Override
    public BigDecimal frete(Compra compra) {
        return Dinheiro.arredondar(base.add(porKg.multiply(compra.pesoKg())));
    }
}
