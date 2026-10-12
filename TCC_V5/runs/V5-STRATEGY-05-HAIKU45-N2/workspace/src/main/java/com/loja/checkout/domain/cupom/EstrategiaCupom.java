package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;
import java.util.List;
import com.loja.checkout.domain.Item;

public interface EstrategiaCupom {
    boolean aplicavel(BigDecimal subtotal, List<Item> itens);
    BigDecimal calcularDesconto(BigDecimal subtotal, List<Item> itens);
    int descricao();
}
