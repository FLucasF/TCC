package br.loja.checkout.entrega;

import br.loja.checkout.Carrinho;
import br.loja.checkout.Dinheiro;
import java.math.BigDecimal;

/** Base para entregas com valor fixo mais um valor por kg. */
abstract class FretePorPeso implements ModalidadeEntrega {
    private final String codigo;
    private final int prazoDias;
    private final BigDecimal base;
    private final BigDecimal porKg;

    FretePorPeso(String codigo, int prazoDias, String base, String porKg) {
        this.codigo = codigo;
        this.prazoDias = prazoDias;
        this.base = new BigDecimal(base);
        this.porKg = new BigDecimal(porKg);
    }

    @Override
    public String codigo() {
        return codigo;
    }

    @Override
    public int prazoDias() {
        return prazoDias;
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.arredondar(base.add(porKg.multiply(carrinho.pesoKg())));
    }
}
