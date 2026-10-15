package com.loja.checkout.strategy;

import java.math.BigDecimal;
import com.loja.checkout.model.ItemRequest;
import com.loja.checkout.util.ArredondadorMeio;
import java.util.List;

public class CupomBemvindo10 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        return ArredondadorMeio.arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
    }

    @Override
    public boolean ehAplicavel(BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public boolean ofereceFretGratis() {
        return false;
    }

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

}
