package com.loja.checkout.cupom;

import com.loja.checkout.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.List;

public interface Cupom {

    boolean aplicavel(List<ItemRequest> itens, BigDecimal subtotalProdutos);

    BigDecimal desconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete);
}
