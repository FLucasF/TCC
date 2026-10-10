package com.loja.model.cupom;

import com.loja.model.ItemCarrinho;
import java.math.BigDecimal;
import java.util.List;

public interface CalculadoraDesconto {
    BigDecimal calcular(BigDecimal subtotal, List<ItemCarrinho> itens);
    boolean ehFretegratis();
}
