package loja.checkout.entrega;

import java.math.BigDecimal;
import loja.checkout.Carrinho;
import loja.checkout.Dinheiro;

abstract class PorPeso implements ModalidadeEntrega {
    private final BigDecimal base;
    private final BigDecimal porKg;

    PorPeso(String base, String porKg) {
        this.base = new BigDecimal(base);
        this.porKg = new BigDecimal(porKg);
    }

    @Override
    public boolean atende(Carrinho carrinho) {
        return true;
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.arredondar(base.add(porKg.multiply(carrinho.pesoKg())));
    }
}
