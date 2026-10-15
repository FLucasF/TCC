package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** Base para entregas que cobram um valor fixo mais um valor por kg do pedido. */
public abstract class EntregaPorPeso implements ModalidadeEntrega {

    private final BigDecimal valorFixo;
    private final BigDecimal valorPorKg;

    protected EntregaPorPeso(String valorFixo, String valorPorKg) {
        this.valorFixo = new BigDecimal(valorFixo);
        this.valorPorKg = new BigDecimal(valorPorKg);
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.arredondar(valorFixo.add(valorPorKg.multiply(pedido.pesoTotalKg())));
    }
}
