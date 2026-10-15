package com.loja.checkout.entrega;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.pedido.Pedido;
import java.math.BigDecimal;

/** Base das transportadoras que cobram um valor fixo mais um valor por quilo. */
public abstract class EntregaPorPeso implements ModalidadeEntrega {

    private final BigDecimal valorFixo;
    private final BigDecimal valorPorKg;

    protected EntregaPorPeso(String valorFixo, String valorPorKg) {
        this.valorFixo = new BigDecimal(valorFixo);
        this.valorPorKg = new BigDecimal(valorPorKg);
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return Dinheiro.arredondar(valorFixo.add(valorPorKg.multiply(pedido.pesoTotalKg())));
    }
}
