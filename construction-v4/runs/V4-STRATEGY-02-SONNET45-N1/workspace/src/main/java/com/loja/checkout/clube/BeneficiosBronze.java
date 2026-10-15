package com.loja.checkout.clube;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class BeneficiosBronze implements BeneficioClube {
    @Override
    public BigDecimal calcularCredito(List<ItemCarrinho> itens) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public boolean freteGratis() {
        return false;
    }

    @Override
    public boolean ganharBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
