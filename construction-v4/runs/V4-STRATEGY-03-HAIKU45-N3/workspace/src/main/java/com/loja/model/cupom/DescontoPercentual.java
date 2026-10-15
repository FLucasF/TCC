package com.loja.model.cupom;

import com.loja.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class DescontoPercentual implements CalculadoraDesconto {
    private final BigDecimal percentual;

    public DescontoPercentual(BigDecimal percentual) {
        this.percentual = percentual;
    }

    @Override
    public BigDecimal calcular(BigDecimal subtotal, List<ItemCarrinho> itens) {
        return subtotal.multiply(percentual);
    }

    @Override
    public boolean ehFretegratis() {
        return false;
    }
}
