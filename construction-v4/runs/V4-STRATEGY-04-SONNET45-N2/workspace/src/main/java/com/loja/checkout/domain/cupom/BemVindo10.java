package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

import static com.loja.checkout.util.Arredondamento.arredondar;

public class BemVindo10 implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return arredondar(subtotalProdutos.multiply(new BigDecimal("0.10")));
    }

    @Override
    public boolean aplicavel(BigDecimal subtotalProdutos) {
        return true;
    }
}
