package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/** Base para opções que cobram um valor fixo mais um valor por kg do pedido. */
abstract class EntregaBaseMaisPeso implements ModalidadeEntrega {

    private final BigDecimal valorFixo;
    private final BigDecimal valorPorKg;

    protected EntregaBaseMaisPeso(String valorFixo, String valorPorKg) {
        this.valorFixo = new BigDecimal(valorFixo);
        this.valorPorKg = new BigDecimal(valorPorKg);
    }

    @Override
    public BigDecimal frete(ContextoEntrega contexto) {
        BigDecimal peso = contexto.carrinho().pesoTotalKg();
        return Dinheiro.arredondar(valorFixo.add(valorPorKg.multiply(peso)));
    }
}
