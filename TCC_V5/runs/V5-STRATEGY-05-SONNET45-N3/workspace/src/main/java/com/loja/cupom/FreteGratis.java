package com.loja.cupom;

import com.loja.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public class FreteGratis implements Cupom {

    @Override
    public BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens) {
        return frete;
    }

    @Override
    public boolean podeAplicar(BigDecimal subtotalProdutos) {
        return true;
    }
}
