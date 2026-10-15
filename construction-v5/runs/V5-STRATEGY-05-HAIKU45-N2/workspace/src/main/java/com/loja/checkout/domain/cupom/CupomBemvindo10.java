package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.Arredondamento;

public class CupomBemvindo10 implements EstrategiaCupom {
    @Override
    public boolean aplicavel(BigDecimal subtotal, List<Item> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotal, List<Item> itens) {
        return Arredondamento.arredondar(subtotal.multiply(new BigDecimal("0.10")));
    }

    @Override
    public int descricao() {
        return 0;
    }
}
