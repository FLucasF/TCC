package com.loja.model.cupom;

import com.loja.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class FreteGratis implements CalculadoraDesconto {
    @Override
    public BigDecimal calcular(BigDecimal subtotal, List<ItemCarrinho> itens) {
        return BigDecimal.ZERO;
    }

    @Override
    public boolean ehFretegratis() {
        return true;
    }
}
