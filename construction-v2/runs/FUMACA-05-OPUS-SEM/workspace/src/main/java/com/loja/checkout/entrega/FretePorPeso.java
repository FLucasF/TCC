package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/** Base para transportadoras que cobram um valor fixo mais um tanto por quilo. */
public abstract class FretePorPeso implements ModalidadeEntrega {

    private final BigDecimal valorFixo;
    private final BigDecimal valorPorKg;

    protected FretePorPeso(String valorFixo, String valorPorKg) {
        this.valorFixo = new BigDecimal(valorFixo);
        this.valorPorKg = new BigDecimal(valorPorKg);
    }

    @Override
    public BigDecimal frete(Pedido pedido) {
        return Dinheiro.centavos(valorFixo.add(valorPorKg.multiply(pedido.pesoKg())));
    }
}
