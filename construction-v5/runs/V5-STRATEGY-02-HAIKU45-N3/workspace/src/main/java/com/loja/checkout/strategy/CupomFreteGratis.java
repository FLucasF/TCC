package com.loja.checkout.strategy;

import java.math.BigDecimal;
import com.loja.checkout.model.ItemRequest;
import com.loja.checkout.util.ArredondadorMeio;
import java.util.List;

public class CupomFreteGratis implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, List<ItemRequest> itens) {
        return ArredondadorMeio.zero();
    }

    @Override
    public boolean ehAplicavel(BigDecimal subtotalProdutos) {
        return true;
    }

    @Override
    public boolean ofereceFretGratis() {
        return true;
    }

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

}
