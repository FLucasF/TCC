package loja.checkout.entrega;

import java.math.BigDecimal;

import loja.checkout.Carrinho;
import loja.checkout.Dinheiro;

/** Valor fixo mais um valor por kg do pedido. */
abstract class FretePorPeso implements ModalidadeEntrega {

    private final BigDecimal base;
    private final BigDecimal porKg;

    FretePorPeso(String base, String porKg) {
        this.base = new BigDecimal(base);
        this.porKg = new BigDecimal(porKg);
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.arredondar(base.add(porKg.multiply(carrinho.pesoKg())));
    }
}
