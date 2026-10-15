package br.com.loja.checkout.entrega;

import br.com.loja.checkout.calculo.Carrinho;
import br.com.loja.checkout.calculo.Dinheiro;
import java.math.BigDecimal;

/** Opções que cobram um valor fixo mais um valor por kg do pedido. */
public abstract class EntregaPorPeso implements ModalidadeEntrega {

    private final BigDecimal valorFixo;
    private final BigDecimal valorPorKg;

    protected EntregaPorPeso(String valorFixo, String valorPorKg) {
        this.valorFixo = new BigDecimal(valorFixo);
        this.valorPorKg = new BigDecimal(valorPorKg);
    }

    @Override
    public BigDecimal calcularFrete(Carrinho carrinho) {
        return Dinheiro.centavos(valorFixo.add(valorPorKg.multiply(carrinho.pesoKg())));
    }
}
