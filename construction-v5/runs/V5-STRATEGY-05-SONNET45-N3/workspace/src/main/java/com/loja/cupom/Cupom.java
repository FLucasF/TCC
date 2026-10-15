package com.loja.cupom;

import com.loja.dto.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens);
    boolean podeAplicar(BigDecimal subtotalProdutos);
}
