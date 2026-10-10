package com.loja.domain.cupom;

import com.loja.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    boolean aplicavel(BigDecimal subtotalProdutos, List<ItemCarrinho> itens);
    ResultadoDesconto calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete, List<ItemCarrinho> itens);
}
