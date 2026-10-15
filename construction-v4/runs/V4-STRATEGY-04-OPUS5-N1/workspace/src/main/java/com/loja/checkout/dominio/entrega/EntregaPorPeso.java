package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Centavos;
import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/** Cobra um valor fixo mais um valor por kg do pedido. */
public record EntregaPorPeso(String codigo, BigDecimal valorFixo, BigDecimal valorPorKg, int prazoDias)
        implements ModalidadeEntrega {

    @Override
    public boolean atende(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Centavos.arredondar(valorFixo.add(valorPorKg.multiply(pedido.pesoKg())));
    }
}
