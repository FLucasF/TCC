package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;

import java.math.BigDecimal;

/** Frete no formato "valor fixo + valor por kg do pedido". */
public abstract class FretePorPeso implements ModalidadeEntrega {

    private final BigDecimal valorFixo;
    private final BigDecimal valorPorKg;

    protected FretePorPeso(String valorFixo, String valorPorKg) {
        this.valorFixo = new BigDecimal(valorFixo);
        this.valorPorKg = new BigDecimal(valorPorKg);
    }

    @Override
    public BigDecimal frete(Carrinho carrinho) {
        return Dinheiro.arredondar(valorFixo.add(valorPorKg.multiply(carrinho.pesoKg())));
    }
}
