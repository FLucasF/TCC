package com.loja.checkout.entrega;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class EntregaRetiradaLoja implements Entrega {
    @Override
    public BigDecimal calcularFrete(List<ItemCarrinho> itens) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int getPrazoEntregaDias() {
        return 1;
    }

    @Override
    public boolean estaDisponivel(List<ItemCarrinho> itens) {
        return true;
    }
}
