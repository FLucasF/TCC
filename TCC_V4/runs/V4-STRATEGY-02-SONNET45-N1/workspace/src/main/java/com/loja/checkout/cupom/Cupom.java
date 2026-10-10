package com.loja.checkout.cupom;

import com.loja.checkout.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface Cupom {
    BigDecimal calcularDesconto(List<ItemCarrinho> itens, BigDecimal frete);
    boolean ehAplicavel(List<ItemCarrinho> itens);
}
