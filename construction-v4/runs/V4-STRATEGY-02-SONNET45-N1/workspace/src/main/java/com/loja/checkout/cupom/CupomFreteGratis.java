package com.loja.checkout.cupom;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class CupomFreteGratis implements Cupom {
    @Override
    public BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal frete) {
        return frete;
    }

    @Override
    public boolean ehAplicavel(List<ItemCarrinho> itens) {
        return true;
    }
}
