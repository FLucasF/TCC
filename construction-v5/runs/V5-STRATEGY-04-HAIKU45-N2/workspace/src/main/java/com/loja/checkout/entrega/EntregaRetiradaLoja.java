package com.loja.checkout.entrega;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.model.Item;

public class EntregaRetiradaLoja implements CalculadoraEntrega {
    @Override
    public BigDecimal calcular(List<Item> itens) {
        return new BigDecimal("0.00");
    }

    @Override
    public int getPrazo() {
        return 1;
    }

    @Override
    public boolean podeEntregar(List<Item> itens) {
        return true;
    }
}
