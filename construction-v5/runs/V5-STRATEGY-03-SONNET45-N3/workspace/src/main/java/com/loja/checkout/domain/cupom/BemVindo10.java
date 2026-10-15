package com.loja.checkout.domain.cupom;

import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class BemVindo10 implements Cupom {
    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens) {
        return true;
    }

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return subtotalProdutos.multiply(new BigDecimal("0.10"))
            .setScale(2, RoundingMode.HALF_EVEN);
    }
}
