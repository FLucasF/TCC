package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.carrinho.Carrinho;

import java.math.BigDecimal;

/** Modalidade cobrada como valor fixo + valor por kg do pedido. */
public abstract class FretePorPeso implements ModalidadeEntrega {

    private final BigDecimal valorBase;
    private final BigDecimal valorPorKg;

    protected FretePorPeso(BigDecimal valorBase, BigDecimal valorPorKg) {
        this.valorBase = valorBase;
        this.valorPorKg = valorPorKg;
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.centavos(valorBase.add(valorPorKg.multiply(carrinho.pesoKg())));
    }
}
