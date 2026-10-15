package com.loja.checkout.dominio.entrega;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** Base das modalidades que cobram uma taxa fixa mais um valor por kg do pedido. */
public abstract class EntregaPorPeso implements ModalidadeEntrega {

    private final BigDecimal taxaFixa;
    private final BigDecimal valorPorKg;

    protected EntregaPorPeso(String taxaFixa, String valorPorKg) {
        this.taxaFixa = new BigDecimal(taxaFixa);
        this.valorPorKg = new BigDecimal(valorPorKg);
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return Dinheiro.centavos(taxaFixa.add(valorPorKg.multiply(pedido.pesoTotalKg())));
    }
}
